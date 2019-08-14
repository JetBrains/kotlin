import java.net.URI

plugins {
    base
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
}

apply(from = "../../gradle/versions.gradle.kts")

val androidBuildToolsVersion = extra["versions.androidBuildTools"] as String
val androidDxSourcesVersion = extra["versions.androidDxSources"] as String

group = "org.jetbrains.kotlin"
version = androidBuildToolsVersion

repositories {
    ivy {
        url = URI("https://dl.google.com/android/repository")

        patternLayout {
            artifact("[artifact]_[revision](-[classifier]).[ext]")
        }

        metadataSources {
            artifact()
        }
    }


    ivy {
        url = URI("https://android.googlesource.com/platform/dalvik/+archive/android-$androidDxSourcesVersion")

        patternLayout {
            artifact("[artifact].[ext]")
        }

        metadataSources {
            artifact()
        }
    }
}

val androidBuildTools by configurations.creating
val androidDxSources by configurations.creating

val androidDxRevision = androidBuildToolsVersion

dependencies {
    androidBuildTools("google:build-tools:$androidBuildToolsVersion:linux@zip")
    androidDxSources("google:dx:0@tar.gz")
}

val unzipDxJar by tasks.registering {
    val outputDir = File(buildDir, name)
    val outputFile = File(outputDir, "dx.jar")

    dependsOn(androidBuildTools)
    inputs.files(androidBuildTools)
    outputs.files(outputFile)

    doFirst {
        project.copy {
            from(zipTree(androidBuildTools.singleFile).files)
            include("**/dx.jar")
            into(outputDir)
        }
    }
}

val untarDxSources by tasks.registering {
    val dxSourcesTargetDir = File(buildDir, name)
    dependsOn(androidDxSources)
    inputs.files(androidDxSources)
    outputs.dir(dxSourcesTargetDir)
    doFirst {
        project.copy {
            from(tarTree(androidDxSources.singleFile))
            include("src/**")
            includeEmptyDirs = false
            into(dxSourcesTargetDir)
        }
    }
}

val prepareDxSourcesJar by tasks.registering(Jar::class) {
    dependsOn(untarDxSources)
    from(untarDxSources)

    archiveClassifier.set("sources")
}

val dxArtifact = artifacts.add("default", unzipDxJar.outputs.files.singleFile) {
    builtBy(unzipDxJar)
}

artifacts.add("archives", dxArtifact)
artifacts.add("archives", prepareDxSourcesJar)

apply(from="../publishing.gradle.kts")
