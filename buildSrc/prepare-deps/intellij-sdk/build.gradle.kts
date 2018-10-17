@file:Suppress("PropertyName")

import org.gradle.api.publish.ivy.internal.artifact.FileBasedIvyArtifact
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyConfiguration
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyPublicationIdentity
import org.gradle.api.publish.ivy.internal.publisher.IvyDescriptorFileGenerator
import java.io.File
import org.gradle.internal.os.OperatingSystem

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

val intellijUltimateEnabled: Boolean by rootProject.extra
val intellijReleaseType: String by rootProject.extra
val intellijVersion = rootProject.extra["versions.intellijSdk"] as String
val androidStudioRelease = rootProject.findProperty("versions.androidStudioRelease") as String?
val androidStudioBuild = rootProject.findProperty("versions.androidStudioBuild") as String?
val intellijSeparateSdks: Boolean by rootProject.extra
val installIntellijCommunity = !intellijUltimateEnabled || intellijSeparateSdks
val installIntellijUltimate = intellijUltimateEnabled

val intellijVersionDelimiterIndex = intellijVersion.indexOfAny(charArrayOf('.', '-'))
if (intellijVersionDelimiterIndex == -1) {
    error("Invalid IDEA version $intellijVersion")
}

val platformBaseVersion = intellijVersion.substring(0, intellijVersionDelimiterIndex)

logger.info("intellijUltimateEnabled: $intellijUltimateEnabled")

logger.info("intellijVersion: $intellijVersion")
logger.info("androidStudioRelease: $androidStudioRelease")
logger.info("androidStudioBuild: $androidStudioBuild")

logger.info("intellijSeparateSdks: $intellijSeparateSdks")
logger.info("installIntellijCommunity: $installIntellijCommunity")
logger.info("installIntellijUltimate: $installIntellijUltimate")

val studioOs by lazy {
    when {
        OperatingSystem.current().isWindows -> "windows"
        OperatingSystem.current().isMacOsX -> "mac"
        OperatingSystem.current().isLinux -> "linux"
        else -> {
            logger.error("Unknown operating system for android tools: ${OperatingSystem.current().name}")
            ""
        }
    }
}

repositories {
    if (androidStudioRelease != null) {
        ivy {
            artifactPattern("https://dl.google.com/dl/android/studio/ide-zips/$androidStudioRelease/[artifact]-[revision]-$studioOs.zip")
            metadataSources {
                artifact()
            }
        }
    }

    if (cacheRedirectorEnabled) {
        maven("https://cache-redirector.jetbrains.com/www.jetbrains.com/intellij-repository/$intellijReleaseType")
        maven("https://cache-redirector.jetbrains.com/plugins.jetbrains.com/maven")
    }

    maven("https://www.jetbrains.com/intellij-repository/$intellijReleaseType")
    maven("https://plugins.jetbrains.com/maven")
}

val intellij by configurations.creating
val intellijUltimate by configurations.creating
val sources by configurations.creating
val `jps-standalone` by configurations.creating
val `jps-build-test` by configurations.creating
val `intellij-core` by configurations.creating
val `plugins-NodeJS` by configurations.creating

val customDepsRepoDir = File(buildDir, "repo")
val customDepsOrg: String by rootProject.extra
val customDepsRevision = intellijVersion
val customDepsRepoModulesDir = File(customDepsRepoDir, "$customDepsOrg/$customDepsRevision")
val repoDir = customDepsRepoModulesDir

dependencies {
    if (androidStudioRelease != null) {
        intellij("google:android-studio-ide:$androidStudioBuild")
    } else {
        if (installIntellijCommunity) {
            intellij("com.jetbrains.intellij.idea:ideaIC:$intellijVersion")
        }
        if (installIntellijUltimate) {
            intellijUltimate("com.jetbrains.intellij.idea:ideaIU:$intellijVersion")
        }
    }
    sources("com.jetbrains.intellij.idea:ideaIC:$intellijVersion:sources@jar")
    `jps-standalone`("com.jetbrains.intellij.idea:jps-standalone:$intellijVersion")
    `jps-build-test`("com.jetbrains.intellij.idea:jps-build-test:$intellijVersion")
    `intellij-core`("com.jetbrains.intellij.idea:intellij-core:$intellijVersion")
    if (intellijUltimateEnabled) {
        `plugins-NodeJS`("com.jetbrains.plugins:NodeJS:${rootProject.extra["versions.idea.NodeJS"]}@zip")
    }
}

fun Task.configureExtractFromConfigurationTask(sourceConfig: Configuration,
                                               pathRemap: (String) -> String = { it },
                                               extractor: (Configuration) -> Any) {
    dependsOn(sourceConfig)
    inputs.files(sourceConfig)
    val targetDir = File(repoDir, sourceConfig.name)
    outputs.dirs(targetDir)
    doFirst {
        project.copy {
            from(extractor(sourceConfig))
            into(targetDir)
            eachFile {
                path = pathRemap(path)
            }
        }
    }
}

fun removePathPrefix(path: String): String {
    if (androidStudioRelease == null) return path
    val slashes = if (studioOs == "mac") 2 else 1
    var result = path
    repeat(slashes) {
        result = result.substringAfter('/')
    }
    return result
}

val unzipIntellijSdk by tasks.creating {
    configureExtractFromConfigurationTask(intellij, pathRemap = { removePathPrefix(it) }) {
        zipTree(it.singleFile).matching {
            exclude("**/plugins/Kotlin/**")
        }
    }
}

val unzipIntellijUltimateSdk by tasks.creating {
    configureExtractFromConfigurationTask(intellijUltimate) {
        zipTree(it.singleFile).matching {
            exclude("plugins/Kotlin/**")
        }
    }
}

val unzipIntellijCore by tasks.creating { configureExtractFromConfigurationTask(`intellij-core`) { zipTree(it.singleFile) } }

val unzipJpsStandalone by tasks.creating { configureExtractFromConfigurationTask(`jps-standalone`) { zipTree(it.singleFile) } }

val copyIntellijSdkSources by tasks.creating(Copy::class.java) {
    from(sources)
    into(File(repoDir, sources.name))
}

val copyJpsBuildTest by tasks.creating { configureExtractFromConfigurationTask(`jps-build-test`) { it.singleFile } }

val unzipNodeJSPlugin by tasks.creating { configureExtractFromConfigurationTask(`plugins-NodeJS`) { zipTree(it.singleFile) } }

fun writeIvyXml(moduleName: String, fileName: String, jarFiles: FileCollection, baseDir: File, sourcesJar: File?) {
    with(IvyDescriptorFileGenerator(DefaultIvyPublicationIdentity(customDepsOrg, moduleName, intellijVersion))) {
        addConfiguration(DefaultIvyConfiguration("default"))
        addConfiguration(DefaultIvyConfiguration("sources"))
        jarFiles.asFileTree.files.forEach {
            if (it.isFile && it.extension == "jar") {
                val relativeName = it.toRelativeString(baseDir).removeSuffix(".jar")
                addArtifact(FileBasedIvyArtifact(it, DefaultIvyPublicationIdentity(customDepsOrg, relativeName, intellijVersion)).also { it.conf = "default" })
            }
        }
        if (sourcesJar != null) {
            val sourcesArtifactName = sourcesJar.name.removeSuffix(".jar").substringBefore("-")
            addArtifact(FileBasedIvyArtifact(sourcesJar, DefaultIvyPublicationIdentity(customDepsOrg, sourcesArtifactName, intellijVersion)).also { it.conf = "sources" })
        }
        writeTo(File(customDepsRepoModulesDir, "$fileName.ivy.xml"))
    }
}

val prepareIvyXmls by tasks.creating {
    dependsOn(unzipIntellijCore, unzipJpsStandalone, copyIntellijSdkSources, copyJpsBuildTest)

    val intellijSdkDir = File(repoDir, intellij.name)
    val intellijUltimateSdkDir = File(repoDir, intellijUltimate.name)

    if (installIntellijCommunity) {
        dependsOn(unzipIntellijSdk)
        inputs.dir(intellijSdkDir)
        outputs.file(File(repoDir, "${intellij.name}.ivy.xml"))
    }

    if (installIntellijUltimate) {
        dependsOn(unzipIntellijUltimateSdk)
        inputs.dir(intellijUltimateSdkDir)
        outputs.file(File(repoDir, "${intellijUltimate.name}.ivy.xml"))
    }

    val flatDeps = listOf(`intellij-core`, `jps-standalone`, `jps-build-test`)
    flatDeps.forEach {
        inputs.dir(File(repoDir, it.name))
        outputs.file(File(repoDir, "${it.name}.ivy.xml"))
    }
    inputs.dir(File(repoDir, sources.name))

    if (intellijUltimateEnabled) {
        dependsOn(unzipNodeJSPlugin)
        inputs.dir(File(repoDir, `plugins-NodeJS`.name))
        outputs.file(File(repoDir, "${`plugins-NodeJS`.name}.ivy.xml"))
    }

    doFirst {
        val sourcesFile = if (sources.isEmpty) null else File(repoDir, "${sources.name}/${sources.singleFile.name}")

        if (installIntellijCommunity) {
            val libDir = File(intellijSdkDir, "lib")
            writeIvyXml(intellij.name,
                        intellij.name,
                        fileTree(libDir).filter {
                            it.parentFile == libDir && !it.name.startsWith("kotlin-")
                        },
                        libDir,
                        sourcesFile)

            File(intellijSdkDir, "plugins").listFiles { it: File -> it.isDirectory }.forEach {
                writeIvyXml(it.name, "intellij.plugin.${it.name}", files("$it/lib/"), File(it, "lib"), sourcesFile)
            }
        }

        if (installIntellijUltimate) {
            val libDir = File(intellijUltimateSdkDir, "lib")
            writeIvyXml(intellij.name, // important! the module name should be "intellij"
                        intellijUltimate.name,
                        fileTree(libDir).filter {
                            it.parentFile == libDir && !it.name.startsWith("kotlin-")
                        },
                        libDir,
                        sourcesFile)

            File(intellijUltimateSdkDir, "plugins").listFiles { it: File -> it.isDirectory }.forEach {
                writeIvyXml(it.name, "intellijUltimate.plugin.${it.name}", files("$it/lib/"), File(it, "lib"), sourcesFile)
            }
        }

        flatDeps.forEach {
            writeIvyXml(it.name, it.name, files("$repoDir/${it.name}"), File(repoDir, it.name), sourcesFile)
        }

        if (intellijUltimateEnabled) {
            val nodeJsBaseDir = "${`plugins-NodeJS`.name}/NodeJS/lib"
            writeIvyXml("NodeJS", `plugins-NodeJS`.name, files("$repoDir/$nodeJsBaseDir"), File(repoDir, nodeJsBaseDir), sourcesFile)
        }
    }
}

val build by tasks.creating {
    dependsOn(prepareIvyXmls)
}

val clean by tasks.creating(Delete::class) {
    delete(customDepsRepoModulesDir)
    delete(buildDir)
}
