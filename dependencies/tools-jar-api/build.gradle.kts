import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassReader.SKIP_CODE
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.jetbrains.org.objectweb.asm.Opcodes.API_VERSION
import java.util.zip.ZipFile

plugins {
    base
    `java-base`
}

val runtimeElements by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

val toolsJarStubs by tasks.registering {
    val toolsJarFile = toolsJar().singleFile
    inputs.file(toolsJarFile)

    val outDir = layout.buildDirectory.dir(name)
    outputs.dir(outDir)

    val usedInternalApiPackages = listOf(
        "com/sun/tools/javac" // Used in KAPT
    )

    doLast {
        val outputDirectoryFile = outDir.get().asFile
        outputDirectoryFile.deleteRecursively()
        val zipFile = ZipFile(toolsJarFile)
        zipFile.stream()
            .filter { it.name.endsWith(".class") }
            .forEach { zipEntry ->
                zipFile.getInputStream(zipEntry).use { entryStream ->
                    val classReader = ClassReader(entryStream)
                    val classWriter = ClassWriter( 0)
                    var isExported = false
                    classReader.accept(object : ClassVisitor(API_VERSION, classWriter) {
                        override fun visit(
                            version: Int,
                            access: Int,
                            name: String?,
                            signature: String?,
                            superName: String?,
                            interfaces: Array<out String>?
                        ) {
                            val isPublic = access and ACC_PUBLIC != 0
                            if (isPublic && usedInternalApiPackages.any { name?.startsWith(it) == true }) {
                                isExported = true
                            }

                            super.visit(version, access, name, signature, superName, interfaces)
                        }
                        override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
                            if (descriptor == "Ljdk/Exported;") {
                                isExported = true
                            }

                            return super.visitAnnotation(descriptor, visible)
                        }
                    }, SKIP_CODE)

                    if (isExported) {
                        val result = File(outputDirectoryFile, zipEntry.name)
                        result.parentFile.mkdirs()
                        result.writeBytes(classWriter.toByteArray())
                    }
                }
            }
    }
}

val jar = tasks.register<Jar>("jar") {
    dependsOn(toolsJarStubs)
    from {
        fileTree(toolsJarStubs.get().outputs.files.singleFile)
    }
}

artifacts.add(runtimeElements.name, jar)
