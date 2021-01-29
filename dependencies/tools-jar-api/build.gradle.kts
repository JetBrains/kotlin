import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.ClassReader.SKIP_CODE
import org.jetbrains.org.objectweb.asm.Opcodes.*
import java.util.zip.ZipFile

plugins {
    base
}

val runtimeElements by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

val JDK_18: String by rootProject.extra

val toolsJarStubs by tasks.registering {
    val toolsJarFile = toolsJarFile(jdkHome = File(JDK_18)) ?: error("Couldn't find tools.jar in $JDK_18")
    inputs.file(toolsJarFile)

    val outDir = buildDir.resolve(name)
    outputs.dir(outDir)

    val usedInternalApiPackages = listOf(
        "com/sun/tools/javac" // Used in KAPT
    )

    doLast {
        outDir.deleteRecursively()
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
                        val result = File(outDir, zipEntry.name)
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
