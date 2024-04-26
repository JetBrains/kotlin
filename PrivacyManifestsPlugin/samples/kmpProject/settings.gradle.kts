dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        maven("file://${rootDir.parentFile.parentFile.resolve("repo").canonicalPath}")
        gradlePluginPortal()
    }
}