plugins {
    base
}

val JDK_18: String by rootProject.extra
val toolsJarFile = toolsJarFile(jdkHome = File(JDK_18)) ?: error("Couldn't find tools.jar in $JDK_18")

// tools.jar from JDK has different public api on different platforms which makes impossible to reuse caches
// for tasks which depend on it. Since we can't compile against those classes & stay cross-platform anyway,
// we may just exclude them from compile classpath. This should make tools.jar compatible at least within
// one build of JDK for different platforms

val jar = tasks.register<Jar>("jar") {
    from {
        zipTree(toolsJarFile).matching {
            exclude("META-INF/**")

            exclude("sun/tools/attach/LinuxAttachProvider.class")
            exclude("sun/tools/attach/LinuxVirtualMachine${'$'}SocketInputStream.class")
            exclude("sun/tools/attach/LinuxVirtualMachine.class")

            exclude("sun/tools/attach/BsdAttachProvider.class")
            exclude("sun/tools/attach/BsdVirtualMachine${'$'}SocketInputStream.class")
            exclude("sun/tools/attach/BsdVirtualMachine.class")

            exclude("sun/tools/attach/WindowsAttachProvider.class")
            exclude("sun/tools/attach/WindowsVirtualMachine${'$'}SocketInputStream.class")
            exclude("sun/tools/attach/WindowsVirtualMachine.class")

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
        }
    }
}

artifacts.add("default", jar)
