@file:Suppress("PropertyName")

import org.gradle.api.publish.ivy.internal.artifact.FileBasedIvyArtifact
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyConfiguration
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyPublicationIdentity
import org.gradle.api.publish.ivy.internal.publisher.IvyDescriptorFileGenerator
import org.gradle.internal.os.OperatingSystem

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

val intellijUltimateEnabled: Boolean by rootProject.extra
val intellijReleaseType: String by rootProject.extra
val intellijVersion = rootProject.extra["versions.intellijSdk"] as String
val asmVersion = rootProject.findProperty("versions.jar.asm-all") as String?
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
            if (cacheRedirectorEnabled) {
                artifactPattern("https://cache-redirector.jetbrains.com/dl.google.com/dl/android/studio/ide-zips/$androidStudioRelease/[artifact]-[revision]-$studioOs.zip")
            }

            artifactPattern("https://dl.google.com/dl/android/studio/ide-zips/$androidStudioRelease/[artifact]-[revision]-$studioOs.zip")
            metadataSources {
                artifact()
            }
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
val jpsBuildTest by configurations.creating
val intellijCore by configurations.creating
val nodeJS by configurations.creating

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
    jpsBuildTest("com.jetbrains.intellij.idea:jps-build-test:$intellijVersion")
    intellijCore("com.jetbrains.intellij.idea:intellij-core:$intellijVersion")
    if (intellijUltimateEnabled) {
        nodeJS("com.jetbrains.plugins:NodeJS:${rootProject.extra["versions.idea.NodeJS"]}@zip")
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
        if (studioOs == "mac") 
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
        buildIvyRepositoryTask(jpsBuildTest, customDepsOrg, customDepsRepoDir, null, sourcesFile),
        makeIntellijAnnotations
    )

    if (installIntellijUltimate) {
        dependsOn(
            buildIvyRepositoryTask(nodeJS, customDepsOrg, customDepsRepoDir, ::skipToplevelDirectory, sourcesFile)
        )
    }
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

    outputs.upToDateWhen {
        configuration.resolvedConfiguration.resolvedArtifacts.single()
            .let { it.moduleDirectory() }
            .exists()
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
