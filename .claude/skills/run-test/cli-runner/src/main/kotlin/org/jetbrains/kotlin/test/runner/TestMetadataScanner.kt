package org.jetbrains.kotlin.test.runner

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.readLines
import kotlin.io.path.walk
import kotlin.io.path.writeText

data class ScannedClass(
    val className: String,
    val testMetadataValue: String?,
    val testDataPathValue: String?,
    val outerClassName: String?,
    val methods: List<ScannedMethod>,
    val classFilePath: Path,
)

data class ScannedMethod(
    val name: String,
    val testMetadataValue: String?,
)

private const val TEST_METADATA_DESCRIPTOR = "Lorg/jetbrains/kotlin/test/TestMetadata;"
private const val TEST_DATA_PATH_DESCRIPTOR = "Lcom/intellij/testFramework/TestDataPath;"

private const val CACHE_FILE_NAME = "test-class-dirs.cache"

private val SKIP_DIR_NAMES =
    setOf(
        ".git",
        ".gradle",
        ".idea",
        ".kotlin",
        "src",
        "resources",
        "node_modules",
    )

fun scanTestClasses(
    projectRoot: Path,
    verbose: Boolean = false,
): Map<String, ScannedClass> {
    // Phase 1: Find test class directories (cached or discovered via pruned walk)
    val testClassDirs = getTestClassDirectories(projectRoot, verbose)
    if (testClassDirs.isEmpty()) {
        if (verbose) println("[Scanner] No test class directories found")
        return emptyMap()
    }
    if (verbose) println("[Scanner] Scanning ${testClassDirs.size} test class directories")

    // Phase 2: Collect all class files from discovered directories only
    val classFiles =
        testClassDirs.flatMap { dir ->
            dir.walk().filter { it.isRegularFile() && it.toString().endsWith(".class") }.toList()
        }
    if (verbose) println("[Scanner] Found ${classFiles.size} class files to scan")

    // Phase 3: Parallel class file scanning
    val result = ConcurrentHashMap<String, ScannedClass>()
    classFiles.parallelStream().forEach { classFile ->
        val scannedClass = scanClassFile(classFile)
        if (scannedClass != null) {
            result[scannedClass.className] = scannedClass
        }
    }

    if (verbose) println("[Scanner] Scanned ${result.size} classes with test metadata")
    return result
}

private fun getTestClassDirectories(
    projectRoot: Path,
    verbose: Boolean,
): List<Path> {
    val cacheFile = projectRoot.resolve("build").resolve(CACHE_FILE_NAME)

    val cached = loadCachedDirectories(cacheFile, verbose)
    if (cached != null) return cached

    val dirs = discoverTestClassDirectories(projectRoot, verbose)

    saveCachedDirectories(cacheFile, dirs)
    return dirs
}

private fun loadCachedDirectories(
    cacheFile: Path,
    verbose: Boolean,
): List<Path>? {
    if (!cacheFile.exists()) return null
    return try {
        val dirs = cacheFile.readLines().filter(String::isNotBlank).map(Path::of)
        if (dirs.isNotEmpty() && dirs.all { it.isDirectory() }) {
            if (verbose) println("[Scanner] Loaded ${dirs.size} test class directories from cache")
            dirs
        } else {
            if (verbose) println("[Scanner] Cache invalidated (directories missing)")
            null
        }
    } catch (e: Exception) {
        if (verbose) println("[Scanner] Failed to read cache: ${e.message}")
        null
    }
}

private fun saveCachedDirectories(
    cacheFile: Path,
    dirs: List<Path>,
) {
    try {
        cacheFile.parent?.createDirectories()
        cacheFile.writeText(dirs.joinToString("\n") { it.toAbsolutePath().toString() } + "\n")
    } catch (_: Exception) {
        // Best-effort caching
    }
}

/**
 * Walks the project tree with aggressive pruning to find `build/classes/<lang>/test` directories.
 *
 * Pruning rules:
 * - Skip `.git`, `.gradle`, `.idea`, `.kotlin`, `src`, `resources`, `node_modules`
 * - Inside `build/`: only enter `classes` subdirectory
 * - Inside `build/classes/<lang>/`: capture `test`, skip everything else
 */
private fun discoverTestClassDirectories(
    projectRoot: Path,
    verbose: Boolean,
): List<Path> {
    val result = mutableListOf<Path>()

    Files.walkFileTree(
        projectRoot,
        object : SimpleFileVisitor<Path>() {
            @Suppress("detekt:ReturnCount")
            override fun preVisitDirectory(
                dir: Path,
                attrs: BasicFileAttributes,
            ): FileVisitResult {
                val name = dir.fileName?.toString() ?: return FileVisitResult.CONTINUE

                if (name in SKIP_DIR_NAMES) return FileVisitResult.SKIP_SUBTREE

                // Inside build/: only enter "classes" subdirectory
                val parentName = dir.parent?.fileName?.toString()
                if (parentName == "build" && name != "classes") return FileVisitResult.SKIP_SUBTREE

                // Inside build/classes/<lang>/: capture "test" and skip everything else
                val grandparentName =
                    dir.parent
                        ?.parent
                        ?.fileName
                        ?.toString()
                val greatGrandparentName =
                    dir.parent
                        ?.parent
                        ?.parent
                        ?.fileName
                        ?.toString()
                if (grandparentName == "classes" && greatGrandparentName == "build") {
                    if (name == "test") result.add(dir)
                    return FileVisitResult.SKIP_SUBTREE
                }

                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(
                file: Path,
                exc: IOException,
            ): FileVisitResult = FileVisitResult.SKIP_SUBTREE
        },
    )

    if (verbose) {
        println("[Scanner] Discovered ${result.size} test class directories:")
        for (dir in result) {
            println("[Scanner]   ${projectRoot.relativize(dir)}")
        }
    }

    return result
}

private fun scanClassFile(classFile: Path): ScannedClass? {
    val bytes = classFile.readBytes()
    val reader = ClassReader(bytes)

    var className: String? = null
    var testMetadataValue: String? = null
    var testDataPathValue: String? = null
    var outerClassName: String? = null
    val methods = mutableListOf<ScannedMethod>()

    val visitor =
        object : ClassVisitor(Opcodes.ASM9) {
            override fun visit(
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?,
            ) {
                className = name
            }

            override fun visitOuterClass(
                owner: String,
                name: String?,
                descriptor: String?,
            ) {
                outerClassName = owner
            }

            override fun visitAnnotation(
                descriptor: String,
                visible: Boolean,
            ): AnnotationVisitor? =
                when (descriptor) {
                    TEST_METADATA_DESCRIPTOR -> stringValueCollector { testMetadataValue = it }
                    TEST_DATA_PATH_DESCRIPTOR -> stringValueCollector { testDataPathValue = it }
                    else -> null
                }

            override fun visitMethod(
                access: Int,
                name: String,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?,
            ): MethodVisitor? {
                if (!name.startsWith("test")) return null

                return object : MethodVisitor(Opcodes.ASM9) {
                    var methodTestMetadataValue: String? = null

                    override fun visitAnnotation(
                        descriptor: String,
                        visible: Boolean,
                    ): AnnotationVisitor? {
                        if (descriptor == TEST_METADATA_DESCRIPTOR) {
                            return stringValueCollector { methodTestMetadataValue = it }
                        }
                        return null
                    }

                    override fun visitEnd() {
                        methods.add(ScannedMethod(name, methodTestMetadataValue))
                    }
                }
            }
        }

    reader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)

    val resolvedClassName = className
    val hasTestContent = testMetadataValue != null || methods.isNotEmpty()
    if (resolvedClassName == null || !hasTestContent) return null

    return ScannedClass(
        className = resolvedClassName,
        testMetadataValue = testMetadataValue,
        testDataPathValue = testDataPathValue,
        outerClassName = outerClassName,
        methods = methods,
        classFilePath = classFile,
    )
}

private fun stringValueCollector(setter: (String) -> Unit): AnnotationVisitor =
    object : AnnotationVisitor(Opcodes.ASM9) {
        override fun visit(
            name: String?,
            value: Any?,
        ) {
            if (name == "value" && value is String) {
                setter(value)
            }
        }
    }
