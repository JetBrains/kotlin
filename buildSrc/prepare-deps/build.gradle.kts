@file:Suppress("PropertyName", "HasPlatformType", "UnstableApiUsage")

import org.gradle.internal.os.OperatingSystem
import java.io.Closeable
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.stream.XMLOutputFactory

import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask

plugins {
    base
}

val intellijUltimateEnabled: Boolean by rootProject.extra
val intellijReleaseType: String by rootProject.extra
val intellijVersion = rootProject.extra["versions.intellijSdk"] as String
val asmVersion = rootProject.findProperty("versions.jar.asm-all") as String?
val androidStudioRelease = rootProject.findProperty("versions.androidStudioRelease") as String?
val androidStudioBuild = rootProject.findProperty("versions.androidStudioBuild") as String?
val intellijSeparateSdks: Boolean by rootProject.extra
val installIntellijCommunity = !intellijUltimateEnabled || intellijSeparateSdks
val installIntellijUltimate = intellijUltimateEnabled && androidStudioRelease == null

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

        ivy {
            url = URI("https://dl.bintray.com/kotlin/as/")

            patternLayout {
                artifact("[artifact]-[revision]-$androidStudioOs.[ext]")
            }

            credentials {
                username = System.getenv("AS_BINTRAY_USER_NAME") ?: findProperty("bintray.user") as String?
                password = System.getenv("AS_BINTRAY_API_KEY") ?: findProperty("bintray.apikey") as String?
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

val dependenciesDir = (findProperty("kotlin.build.dependencies.dir") as String?)?.let(::File)
    ?: rootProject.gradle.gradleUserHomeDir.resolve("kotlin-build-dependencies")

val customDepsRepoDir = dependenciesDir.resolve("repo")

val customDepsOrg: String by rootProject.extra
val customDepsRevision = intellijVersion
val repoDir = File(customDepsRepoDir, customDepsOrg)

dependencies {
    if (androidStudioRelease != null) {
        val extension = if (androidStudioOs == "linux")
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


val cleanupIntellijCore = tasks.register<CleanDataTask>("cleanupIntellijCore") {
    cleanableStoreProvider = provider { CleanableStore[repoDir.resolve("intellij-core").absolutePath] }
}

val cleanupIntellijAnnotation = tasks.register<CleanDataTask>("cleanupIntellijAnnotation") {
    cleanableStoreProvider = provider { CleanableStore[repoDir.resolve(intellijRuntimeAnnotations).absolutePath] }
}

val cleanupDependencies = tasks.register("cleanupDependencies") {
    dependsOn(cleanupIntellijCore)
    dependsOn(cleanupIntellijAnnotation)
}

val makeIntellijCore = buildIvyRepositoryTaskAndRegisterCleanupTask(intellijCore, customDepsOrg, customDepsRepoDir)

val makeIntellijAnnotations by tasks.registering(Copy::class) {
    dependsOn(makeIntellijCore)

    val intellijCoreRepo = CleanableStore[repoDir.resolve("intellij-core").absolutePath][intellijVersion].use()
    from(intellijCoreRepo.resolve("artifacts/annotations.jar"))

    val targetDir = CleanableStore[repoDir.resolve(intellijRuntimeAnnotations).absolutePath][intellijVersion].use()
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
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    from(provider { sources.map(::zipTree) })
    destinationDirectory.set(File(repoDir, sources.name))
    archiveBaseName.set("intellij")
    archiveClassifier.set("sources")
    archiveVersion.set(intellijVersion)
}

val sourcesFile = mergeSources.outputs.files.singleFile

val makeIde = if (androidStudioBuild != null) {
    buildIvyRepositoryTaskAndRegisterCleanupTask(
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
        buildIvyRepositoryTaskAndRegisterCleanupTask(intellijUltimate, customDepsOrg, customDepsRepoDir, null, sourcesFile)
    } else {
        buildIvyRepositoryTaskAndRegisterCleanupTask(intellij, customDepsOrg, customDepsRepoDir, null, sourcesFile)
    }

    task.configure {
        dependsOn(mergeSources)
    }

    task
}

val buildJpsStandalone = buildIvyRepositoryTaskAndRegisterCleanupTask(jpsStandalone, customDepsOrg, customDepsRepoDir, null, sourcesFile)

tasks.named("build") {
    dependsOn(
        makeIntellijCore,
        makeIde,
        buildJpsStandalone,
        makeIntellijAnnotations
    )

}

if (installIntellijUltimate) {
    val buildNodeJsPlugin =
        buildIvyRepositoryTaskAndRegisterCleanupTask(nodeJSPlugin, customDepsOrg, customDepsRepoDir, ::skipToplevelDirectory, sourcesFile)
    tasks.named("build") { dependsOn(buildNodeJsPlugin) }
}

tasks.named("build") { dependsOn(cleanupDependencies) }

// Task to delete legacy repo locations
tasks.register<Delete>("cleanLegacy") {
    delete("$projectDir/android-dx")
    delete("$projectDir/intellij-sdk")
}

tasks.named<Delete>("clean") {
    //TODO specify repos to clean? Use CleanDataTask
    delete(customDepsRepoDir)
}

fun buildIvyRepositoryTaskAndRegisterCleanupTask(
    configuration: Configuration,
    organization: String,
    repoDirectory: File,
    pathRemap: ((String) -> String)? = null,
    sources: File? = null
): TaskProvider<Task> {
    fun ResolvedArtifact.storeDirectory(): CleanableStore =
        CleanableStore[repoDirectory.resolve("$organization/${moduleVersion.id.name}").absolutePath]

    fun ResolvedArtifact.moduleDirectory(): File =
        storeDirectory()[moduleVersion.id.version].use()

    val buildIvyRepositoryTask = tasks.register("buildIvyRepositoryFor${configuration.name.capitalize()}") {
        dependsOn(configuration)
        inputs.files(configuration)

        outputs.upToDateWhen {
            configuration.resolvedConfiguration.resolvedArtifacts.single()
                .moduleDirectory()
                .exists()
        }

        doFirst {
            val artifact = configuration.resolvedConfiguration.resolvedArtifacts.single()
            val moduleDirectory = artifact.moduleDirectory()
            if (moduleDirectory.exists()) {
                logger.info("Path ${moduleDirectory.absolutePath} already exists, skipping unpacking.")
                return@doFirst
            }

            with(artifact) {
                val artifactsDirectory = File(moduleDirectory, "artifacts")
                logger.info("Unpacking ${file.name} into ${artifactsDirectory.absolutePath}")
                copy {
                    val fileTree = when (extension) {
                        "tar.gz" -> tarTree(file)
                        "zip" -> zipTree(file)
                        else -> error("Unsupported artifact extension: $extension")
                    }

                    from(
                        fileTree.matching {
                            exclude("**/plugins/Kotlin/**")
                        }
                    )

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

    val cleanupIvyRepositoryTask = tasks.register<CleanDataTask>("cleanupIvyRepositoryFor${configuration.name.capitalize()}") {
        cleanableStoreProvider = provider {
            configuration.resolvedConfiguration.resolvedArtifacts.single().storeDirectory()
        }
    }

    cleanupDependencies {
        dependsOn(cleanupIvyRepositoryTask)
    }

    return buildIvyRepositoryTask
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
    with(XMLWriter(ivyFile.writer())) {
        document("UTF-8", "1.0") {
            element("ivy-module") {
                attribute("version", "2.0")
                attribute("xmlns:m", "http://ant.apache.org/ivy/maven")

                emptyElement("info") {
                    attributes(
                        "organisation" to organization,
                        "module" to moduleName,
                        "revision" to version,
                        "publication" to SimpleDateFormat("yyyyMMddHHmmss").format(Date())
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
                    artifactDir.listFiles()
                        ?.filter(::shouldIncludeIntellijJar)
                        ?.sortedBy { it.name.toLowerCase() }
                        ?.forEach { jarFile ->
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
                            val sourcesArtifactName = jarFile.name.substringBefore("-$version")
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

        close()
    }
}

fun skipToplevelDirectory(path: String) = path.substringAfter('/')

fun skipContentsDirectory(path: String) = path.substringAfter("Contents/")

class XMLWriter(private val outputStreamWriter: OutputStreamWriter) : Closeable {

    private val xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStreamWriter)

    private var depth = 0
    private val indent = "  "

    fun document(encoding: String, version: String, init: XMLWriter.() -> Unit) = apply {
        xmlStreamWriter.writeStartDocument(encoding, version)

        init()

        xmlStreamWriter.writeEndDocument()
    }

    fun element(name: String, init: XMLWriter.() -> Unit) = apply {
        writeIndent()
        xmlStreamWriter.writeStartElement(name)
        depth += 1

        init()

        depth -= 1
        writeIndent()
        xmlStreamWriter.writeEndElement()
    }

    fun emptyElement(name: String, init: XMLWriter.() -> Unit) = apply {
        writeIndent()
        xmlStreamWriter.writeEmptyElement(name)
        init()
    }

    fun attribute(name: String, value: String): Unit = xmlStreamWriter.writeAttribute(name, value)

    fun attributes(vararg attributes: Pair<String, String>) {
        attributes.forEach { attribute(it.first, it.second) }
    }

    private fun writeIndent() {
        xmlStreamWriter.writeCharacters("\n")
        repeat(depth) {
            xmlStreamWriter.writeCharacters(indent)
        }
    }

    override fun close() {
        xmlStreamWriter.flush()
        xmlStreamWriter.close()
        outputStreamWriter.close()
    }
}