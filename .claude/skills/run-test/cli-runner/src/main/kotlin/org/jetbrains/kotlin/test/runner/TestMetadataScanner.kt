package org.jetbrains.kotlin.test.runner

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.walk

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

private val BUILD_CLASSES_TEST_REGEX = Regex("""build/classes/[^/]+/test/""")

fun scanTestClasses(
    projectRoot: Path,
    verbose: Boolean = false,
): Map<String, ScannedClass> {
    val result = mutableMapOf<String, ScannedClass>()

    val classFiles =
        projectRoot
            .walk()
            .filter { it.isRegularFile() && it.toString().endsWith(".class") }
            .filter { BUILD_CLASSES_TEST_REGEX.containsMatchIn(it.toString()) }

    for (classFile in classFiles) {
        val scannedClass = scanClassFile(classFile, verbose)
        if (scannedClass != null) {
            result[scannedClass.className] = scannedClass
        }
    }

    if (verbose) {
        println("[Scanner] Scanned ${result.size} classes with test metadata")
    }

    return result
}

private fun scanClassFile(
    classFile: Path,
    verbose: Boolean,
): ScannedClass? {
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

    if (verbose) {
        println(
            "[Scanner] Found: $resolvedClassName" +
                " (metadata=$testMetadataValue, methods=${methods.size})",
        )
    }

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
