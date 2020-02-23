import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassReader.SKIP_CODE
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes.*
import java.util.zip.ZipFile

plugins {
    base
}

val JDK_18: String by rootProject.extra
val toolsJarFile = toolsJarFile(jdkHome = File(JDK_18)) ?: error("Couldn't find tools.jar in $JDK_18")

// tools.jar from JDK has different public api on different platforms which makes impossible to reuse caches
// for tasks which depend on it. Since we can't compile against those classes & stay cross-platform anyway,
// we may just exclude them from compile classpath. Since method bodies are not required for compilation
// strip them out to remove noise between different versions.

val toolsJarStubs by tasks.registering {
    inputs.file(toolsJarFile)

    val outDir = buildDir.resolve(name)
    outputs.dir(outDir)

    doLast {
        val zipFile = ZipFile(toolsJarFile)
        zipFile.stream()
            .filter { it.name.endsWith(".class") }
            .forEach { zipEntry ->
                zipFile.getInputStream(zipEntry).use { entryStream ->
                    val classReader = ClassReader(entryStream)
                    val classWriter = ClassWriter( 0)
                    classReader.accept(object : ClassVisitor(API_VERSION, classWriter) {

                    }, SKIP_CODE)
                    val result = File(outDir, zipEntry.name)
                    result.ensureParentDirsCreated()
                    result.writeBytes(classWriter.toByteArray())
                }
            }
    }
}

val jar = tasks.register<Jar>("jar") {
    dependsOn(toolsJarStubs)
    from {
        fileTree(toolsJarStubs.get().outputs.files.singleFile).matching {
            exclude("META-INF/**")

            exclude("sun/tools/attach/LinuxAttachProvider.class")
            exclude("sun/tools/attach/LinuxVirtualMachine*")

            exclude("sun/tools/attach/BsdAttachProvider.class")
            exclude("sun/tools/attach/BsdVirtualMachine*")

            exclude("sun/tools/attach/WindowsAttachProvider.class")
            exclude("sun/tools/attach/WindowsVirtualMachine*")

            // Windows only classes
            exclude("com/sun/tools/jdi/SharedMemoryAttachingConnector$1.class")
            exclude("com/sun/tools/jdi/SharedMemoryAttachingConnector.class")
            exclude("com/sun/tools/jdi/SharedMemoryConnection.class")
            exclude("com/sun/tools/jdi/SharedMemoryListeningConnector$1.class")
            exclude("com/sun/tools/jdi/SharedMemoryListeningConnector.class")
            exclude("com/sun/tools/jdi/SharedMemoryTransportService${'$'}SharedMemoryListenKey.class")
            exclude("com/sun/tools/jdi/SharedMemoryTransportService.class")
            exclude("com/sun/tools/jdi/SharedMemoryTransportServiceCapabilities.class")
            exclude("com/sun/tools/jdi/SunSDK.class")

            // Deprecated class which has differences in api between versions
            exclude("com/sun/tools/javadoc/JavaScriptScanner.class")
        }
    }
}

artifacts.add("default", jar)
