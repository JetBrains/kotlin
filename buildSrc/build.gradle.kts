// NOTE: This buildfile file is completely ignored when running composite build `kotlin` + `kotlin-ultimate`.

plugins {
    base
}

rootProject.apply {
    from(rootProject.file("../gradle/cidrPluginProperties.gradle.kts"))
}
