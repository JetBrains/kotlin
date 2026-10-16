plugins {
    id("common-configuration")
    `java-library`
}

jvmToolchains {
    jdkVersion = JdkMajorVersion.JDK_17_0
    targetBytecodeVersion = JdkMajorVersion.JDK_17_0
}

// Opt-in: declare the processor incremental (isolating). Off by default so the nine default cells
// keep their current (NON_INCREMENTAL) behaviour — with a non-incremental processor on the path,
// kapt disables incremental APT entirely.
val incrementalProcessor: Boolean = (findProperty("incrementalProcessor") as String?).toBoolean()
if (incrementalProcessor) {
    sourceSets["main"].resources.srcDir("src/incremental/resources")
}
