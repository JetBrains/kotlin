@file:Suppress("PropertyName", "HasPlatformType", "UnstableApiUsage")

import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter
import org.gradle.internal.os.OperatingSystem
import java.io.FileWriter
import java.net.URI
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

plugins {
    base
}

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

repositories {
    if (androidStudioRelease != null) {
        ivy {
            url = URI("https://dl.google.com/dl/android/studio/ide-zips/$androidStudioRelease")

            patternLayout {
                artifact("[artifact]-[revision]-$androidStudioOs.[ext]")
            }

            metadataSources {
                artifact()
            }
        }
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
        val extension = if (androidStudioOs == "linux" && androidStudioRelease.startsWith("3.5"))
            "tar.gz"
        else
            "zip"

        androidStudio("google:android-studio-ide:$androidStudioBuild@$extension")
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
    destinationDirectory.set(File(repoDir, sources.name))
    archiveBaseName.set("intellij")
    archiveClassifier.set("sources")
    archiveVersion.set(intellijVersion)
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

val buildJpsStandalone = buildIvyRepositoryTask(jpsStandalone, customDepsOrg, customDepsRepoDir, null, sourcesFile)
val buildNodeJsPlugin = buildIvyRepositoryTask(nodeJSPlugin, customDepsOrg, customDepsRepoDir, ::skipToplevelDirectory, sourcesFile)

tasks.named("build") {
    dependsOn(
        makeIntellijCore,
        makeIde,
        buildJpsStandalone,
        makeIntellijAnnotations
    )

    if (installIntellijUltimate) {
        dependsOn(buildNodeJsPlugin)
    }
}

// Task to delete legacy repo locations
tasks.create("cleanLegacy", Delete::class.java) {
    delete("$projectDir/android-dx")
    delete("$projectDir/intellij-sdk")
}

tasks.named<Delete>("clean") {
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
                val fileTree = when (extension) {
                    "tar.gz" -> tarTree(file)
                    "zip" -> zipTree(file)
                    else -> error("Unsupported artifact extension: $extension")
                }

                from(fileTree.matching {
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

    val ivyFile = targetDir.resolve("$fileName.ivy.xml")
    ivyFile.parentFile.mkdirs()
    FileWriter(ivyFile).use {
        val xmlWriter = IndentingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(it))
        with(xmlWriter) {
            document("UTF-8", "1.0") {
                element("ivy-module") {
                    attribute("version", "2.0")
                    attribute("xmlns:m", "http://ant.apache.org/ivy/maven")

                    emptyElement("info") {
                        attributes(
                            "organisation" to organization,
                            "module" to moduleName,
                            "revision" to version,
                            "publication" to ""
                        )
                    }

                    element("configurations") {
                        listOf("default", "sources").forEach { configurationName ->
                            emptyElement("conf") {
                                attributes("name" to configurationName, "visibility" to "public")
                            }
                        }
                    }

                    element("publications") {
                        artifactDir.listFiles()?.filter(::shouldIncludeIntellijJar)?.forEach { jarFile ->
                            val relativeName = jarFile.toRelativeString(baseDir).removeSuffix(".jar")
                            emptyElement("artifact") {
                                attributes(
                                    "name" to relativeName,
                                    "type" to "jar",
                                    "ext" to "jar",
                                    "conf" to "default"
                                )
                            }
                        }

                        sourcesJar.forEach { jarFile ->
                            emptyElement("artifact") {
                                val sourcesArtifactName = jarFile.name
                                    .substringBeforeLast("-")
                                    .substringBeforeLast("-")

                                attributes(
                                    "name" to sourcesArtifactName,
                                    "type" to "jar",
                                    "ext" to "jar",
                                    "conf" to "sources",
                                    "m:classifier" to "sources"
                                )
                            }
                        }
                    }
                }
            }

            flush()
            close()
        }
    }
}

fun skipToplevelDirectory(path: String) = path.substringAfter('/')

fun skipContentsDirectory(path: String) = path.substringAfter("Contents/")

fun XMLStreamWriter.document(encoding: String, version: String, init: XMLStreamWriter.() -> Unit) = apply {
    writeStartDocument(encoding, version)
    init()
    writeEndDocument()
}

fun XMLStreamWriter.element(name: String, init: XMLStreamWriter.() -> Unit) = apply {
    writeStartElement(name)
    init()
    writeEndElement()
}

fun XMLStreamWriter.emptyElement(name: String, init: XMLStreamWriter.() -> Unit) = apply {
    writeEmptyElement(name)
    init()
}

fun XMLStreamWriter.attribute(name: String, value: String): Unit = writeAttribute(name, value)

fun XMLStreamWriter.attributes(vararg attributes: Pair<String, String>) {
    attributes.forEach { attribute(it.first, it.second) }
}
