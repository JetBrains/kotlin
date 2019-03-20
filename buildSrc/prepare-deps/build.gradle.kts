@file:Suppress("PropertyName")

import org.gradle.api.publish.ivy.internal.artifact.FileBasedIvyArtifact
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyConfiguration
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyPublicationIdentity
import org.gradle.api.publish.ivy.internal.publisher.IvyDescriptorFileGenerator
import org.gradle.internal.os.OperatingSystem

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true
val verifyDependencyOutput: Boolean by rootProject.extra
val intellijUltimateEnabled: Boolean by rootProject.extra
val intellijReleaseType: String by rootProject.extra
val intellijVersion = rootProject.extra["versions.intellijSdk"] as String
val asmVersion = rootProject.findProperty("versions.jar.asm-all") as String?
val androidStudioRelease = rootProject.findProperty("versions.androidStudioRelease") as String?
val androidStudioBuild = rootProject.findProperty("versions.androidStudioBuild") as String?
val intellijSeparateSdks: Boolean by rootProject.extra
val installIntellijCommunity = !intellijUltimateEnabled || intellijSeparateSdks
val installIntellijUltimate = intellijUltimateEnabled

val androidBuildToolsVersion = rootProject.extra["versions.androidBuildTools"] as String
val androidDxSourcesVersion = rootProject.extra["versions.androidDxSources"] as String

val intellijVersionDelimiterIndex = intellijVersion.indexOfAny(charArrayOf('.', '-'))
if (intellijVersionDelimiterIndex == -1) {
    error("Invalid IDEA version $intellijVersion")
}

val platformBaseVersion = intellijVersion.substring(0, intellijVersionDelimiterIndex)

logger.info("verifyDependencyOutput: $verifyDependencyOutput")
logger.info("intellijUltimateEnabled: $intellijUltimateEnabled")
logger.info("intellijVersion: $intellijVersion")
logger.info("androidStudioRelease: $androidStudioRelease")
logger.info("androidStudioBuild: $androidStudioBuild")
logger.info("intellijSeparateSdks: $intellijSeparateSdks")
logger.info("installIntellijCommunity: $installIntellijCommunity")
logger.info("installIntellijUltimate: $installIntellijUltimate")

val androidStudioOs by lazy {
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

val androidToolsOs by lazy {
    when {
        OperatingSystem.current().isWindows -> "windows"
        OperatingSystem.current().isMacOsX -> "macosx"
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
            if (cacheRedirectorEnabled) {
                artifactPattern("https://cache-redirector.jetbrains.com/dl.google.com/dl/android/studio/ide-zips/$androidStudioRelease/[artifact]-[revision]-$androidStudioOs.zip")
            }

            artifactPattern("https://dl.google.com/dl/android/studio/ide-zips/$androidStudioRelease/[artifact]-[revision]-$androidStudioOs.zip")
            metadataSources {
                artifact()
            }
        }
    }

    ivy {
        artifactPattern("https://dl.google.com/android/repository/[artifact]_[revision](-[classifier]).[ext]")
        artifactPattern("https://android.googlesource.com/platform/dalvik/+archive/android-$androidDxSourcesVersion/[artifact].[ext]")
        metadataSources {
            artifact()
        }
    }

    if (cacheRedirectorEnabled) {
        maven("https://cache-redirector.jetbrains.com/www.jetbrains.com/intellij-repository/$intellijReleaseType")
        maven("https://cache-redirector.jetbrains.com/plugins.jetbrains.com/maven")
        maven("https://cache-redirector.jetbrains.com/jetbrains.bintray.com/intellij-third-party-dependencies/")
    }

    maven("https://www.jetbrains.com/intellij-repository/$intellijReleaseType")
    maven("https://plugins.jetbrains.com/maven")
    maven("https://jetbrains.bintray.com/intellij-third-party-dependencies/")
}

val intellij by configurations.creating
val intellijUltimate by configurations.creating
val androidStudio by configurations.creating
val sources by configurations.creating
val jpsStandalone by configurations.creating
val intellijCore by configurations.creating
val nodeJSPlugin by configurations.creating
val androidBuildTools by configurations.creating
val androidDxSources by configurations.creating

/**
 * Special repository for annotations.jar required for idea runtime only.
 *
 * See IntellijDependenciesKt.intellijRuntimeAnnotations for more details.
 */
val intellijRuntimeAnnotations = "intellij-runtime-annotations"

val customDepsRepoDir = rootProject.rootDir.parentFile.resolve("dependencies/repo")
val customDepsOrg: String by rootProject.extra
val customDepsRevision = intellijVersion
val repoDir = File(customDepsRepoDir, customDepsOrg)

val androidDxModuleName = "android-dx"
val androidDxRevision = androidBuildToolsVersion
val androidDxRepoModuleDir = File(repoDir, "$androidDxModuleName/$androidDxRevision")

dependencies {
    if (androidStudioRelease != null) {
        androidStudio("google:android-studio-ide:$androidStudioBuild")
    } else {
        if (installIntellijCommunity) {
            intellij("com.jetbrains.intellij.idea:ideaIC:$intellijVersion")
        }
        if (installIntellijUltimate) {
            intellijUltimate("com.jetbrains.intellij.idea:ideaIU:$intellijVersion")
        }
    }

    if (asmVersion != null) {
        sources("org.jetbrains.intellij.deps:asm-all:$asmVersion:sources@jar")
    }

    sources("com.jetbrains.intellij.idea:ideaIC:$intellijVersion:sources@jar")
    jpsStandalone("com.jetbrains.intellij.idea:jps-standalone:$intellijVersion")
    intellijCore("com.jetbrains.intellij.idea:intellij-core:$intellijVersion")
    if (intellijUltimateEnabled) {
        nodeJSPlugin("com.jetbrains.plugins:NodeJS:${rootProject.extra["versions.idea.NodeJS"]}@zip")
    }

    androidBuildTools("google:build-tools:$androidBuildToolsVersion:$androidToolsOs@zip")
    androidDxSources("google:dx:0@tar.gz")
}

val dxSourcesTargetDir = File(buildDir, "dx_src")

val untarDxSources by tasks.creating {
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

val prepareDxSourcesJar by tasks.creating(Jar::class) {
    dependsOn(untarDxSources)
    from("$dxSourcesTargetDir/src")
    destinationDir = File(repoDir, sources.name)
    baseName = androidDxModuleName
    classifier = "sources"
    version = androidBuildToolsVersion
}

val unzipDxJar by tasks.creating {
    dependsOn(androidBuildTools)
    inputs.files(androidBuildTools)
    outputs.files(File(androidDxRepoModuleDir, "dx.jar"))
    doFirst {
        project.copy {
            from(zipTree(androidBuildTools.singleFile).files)
            include("**/dx.jar")
            into(androidDxRepoModuleDir)
        }
    }
}

val buildIvyRepoForAndroidDx by tasks.creating {
    dependsOn(unzipDxJar, prepareDxSourcesJar)
    inputs.files(unzipDxJar, prepareDxSourcesJar)
    outputs.file(File(androidDxRepoModuleDir, "$androidDxModuleName.ivy.xml"))

    doLast {
        writeIvyXml(
            customDepsOrg,
            androidDxModuleName,
            androidBuildToolsVersion,
            androidDxModuleName,
            androidDxRepoModuleDir,
            androidDxRepoModuleDir,
            androidDxRepoModuleDir,
            prepareDxSourcesJar.outputs.files.singleFile
        )
    }
}

val makeIntellijCore = buildIvyRepositoryTask(intellijCore, customDepsOrg, customDepsRepoDir)

val makeIntellijAnnotations by tasks.creating(Copy::class.java) {
    dependsOn(makeIntellijCore)

    from(repoDir.resolve("intellij-core/$intellijVersion/artifacts/annotations.jar"))

    val targetDir = File(repoDir, "$intellijRuntimeAnnotations/$intellijVersion")
    into(targetDir)

    val ivyFile = File(targetDir, "$intellijRuntimeAnnotations.ivy.xml")
    outputs.files(ivyFile)

    doLast {
        writeIvyXml(
            customDepsOrg,
            intellijRuntimeAnnotations,
            intellijVersion,
            intellijRuntimeAnnotations,
            targetDir,
            targetDir,
            targetDir,
            allowAnnotations = true
        )
    }
}

val mergeSources by tasks.creating(Jar::class.java) {
    dependsOn(sources)
    from(provider { sources.map(::zipTree) })
    destinationDir = File(repoDir, sources.name)
    baseName = "intellij"
    classifier = "sources"
    version = intellijVersion
}

val sourcesFile = mergeSources.outputs.files.singleFile

val makeIde = if (androidStudioBuild != null) {
    buildIvyRepositoryTask(
        androidStudio,
        customDepsOrg,
        customDepsRepoDir,
        if (androidStudioOs == "mac")
            ::skipContentsDirectory 
        else 
            ::skipToplevelDirectory
    )
} else {
    val task = if (installIntellijUltimate) {
        buildIvyRepositoryTask(intellijUltimate, customDepsOrg, customDepsRepoDir, null, sourcesFile)
    } else {
        buildIvyRepositoryTask(intellij, customDepsOrg, customDepsRepoDir, null, sourcesFile)
    }

    task.configure {
        dependsOn(mergeSources)
    }

    task
}

val build by tasks.creating {
    dependsOn(
        makeIntellijCore,
        makeIde,
        buildIvyRepositoryTask(jpsStandalone, customDepsOrg, customDepsRepoDir, null, sourcesFile),
        makeIntellijAnnotations,
        buildIvyRepoForAndroidDx
    )

    if (installIntellijUltimate) {
        dependsOn(
            buildIvyRepositoryTask(nodeJSPlugin, customDepsOrg, customDepsRepoDir, ::skipToplevelDirectory, sourcesFile)
        )
    }
}

// Task to delete legacy repo locations
tasks.create("cleanLegacy", Delete::class.java) {
    delete("$projectDir/android-dx")
    delete("$projectDir/intellij-sdk")
}

tasks.create("clean", Delete::class.java) {
    delete(customDepsRepoDir)
}

fun buildIvyRepositoryTask(
    configuration: Configuration,
    organization: String,
    repoDirectory: File,
    pathRemap: ((String) -> String)? = null,
    sources: File? = null
) = tasks.register("buildIvyRepositoryFor${configuration.name.capitalize()}") {

    fun ResolvedArtifact.moduleDirectory(): File =
        File(repoDirectory, "$organization/${moduleVersion.id.name}/${moduleVersion.id.version}")

    dependsOn(configuration)
    inputs.files(configuration)

    if (verifyDependencyOutput) {
        outputs.dir(provider {
            configuration.resolvedConfiguration.resolvedArtifacts.single().moduleDirectory()
        })
    } else {
        outputs.upToDateWhen {
            configuration.resolvedConfiguration.resolvedArtifacts.single()
                .moduleDirectory()
                .exists()
        }
    }

    doFirst {
        configuration.resolvedConfiguration.resolvedArtifacts.single().run {
            val moduleDirectory = moduleDirectory()
            val artifactsDirectory = File(moduleDirectory(), "artifacts")

            logger.info("Unpacking ${file.name} into ${artifactsDirectory.absolutePath}")
            copy {
                from(zipTree(file).matching {
                    exclude("**/plugins/Kotlin/**")
                })

                into(artifactsDirectory)

                if (pathRemap != null) {
                    eachFile {
                        path = pathRemap(path)
                    }
                }

                includeEmptyDirs = false
            }

            writeIvyXml(
                organization,
                moduleVersion.id.name,
                moduleVersion.id.version,
                moduleVersion.id.name,
                File(artifactsDirectory, "lib"),
                File(artifactsDirectory, "lib"),
                File(moduleDirectory, "ivy"),
                *listOfNotNull(sources).toTypedArray()
            )

            val pluginsDirectory = File(artifactsDirectory, "plugins")
            if (pluginsDirectory.exists()) {
                file(File(artifactsDirectory, "plugins"))
                    .listFiles { file: File -> file.isDirectory }
                    .forEach {
                        writeIvyXml(
                            organization,
                            it.name,
                            moduleVersion.id.version,
                            it.name,
                            File(it, "lib"),
                            File(it, "lib"),
                            File(moduleDirectory, "ivy"),
                            *listOfNotNull(sources).toTypedArray()
                        )
                    }
            }
        }
    }
}

fun writeIvyXml(
    organization: String,
    moduleName: String,
    version: String,
    fileName: String,
    baseDir: File,
    artifactDir: File,
    targetDir: File,
    vararg sourcesJar: File,
    allowAnnotations: Boolean = false
) {
    fun shouldIncludeIntellijJar(jar: File) =
        jar.isFile
                && jar.extension == "jar"
                && !jar.name.startsWith("kotlin-")
                && (allowAnnotations || jar.name != "annotations.jar") // see comments for [intellijAnnotations] above

    with(IvyDescriptorFileGenerator(DefaultIvyPublicationIdentity(organization, moduleName, version))) {
        addConfiguration(DefaultIvyConfiguration("default"))
        addConfiguration(DefaultIvyConfiguration("sources"))
        artifactDir.listFiles()?.forEach { jarFile ->
            if (shouldIncludeIntellijJar(jarFile)) {
                val relativeName = jarFile.toRelativeString(baseDir).removeSuffix(".jar")
                addArtifact(
                    FileBasedIvyArtifact(
                        jarFile,
                        DefaultIvyPublicationIdentity(organization, relativeName, version)
                    ).also {
                        it.conf = "default"
                    }
                )
            }
        }

        sourcesJar.forEach {
            val sourcesArtifactName = it.name.substringBeforeLast("-").substringBeforeLast("-")
            addArtifact(
                FileBasedIvyArtifact(
                    it,
                    DefaultIvyPublicationIdentity(organization, sourcesArtifactName, version)
                ).also { artifact ->
                    artifact.conf = "sources"
                    artifact.classifier = "sources"
                }
            )
        }

        writeTo(File(targetDir, "$fileName.ivy.xml"))
    }
}

fun skipToplevelDirectory(path: String) = path.substringAfter('/')

fun skipContentsDirectory(path: String) = path.substringAfter("Contents/")
