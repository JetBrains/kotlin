@file:Suppress("PropertyName")

import org.gradle.api.publish.ivy.internal.artifact.DefaultIvyArtifact
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyConfiguration
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyPublicationIdentity
import org.gradle.api.publish.ivy.internal.publisher.IvyDescriptorFileGenerator
import java.io.File
import org.gradle.internal.os.OperatingSystem

val intellijRepo: String by rootProject.extra
val intellijReleaseType: String by rootProject.extra
val intellijSdkDependencyName: String by rootProject.extra
val intellijVersion = rootProject.extra["versions.intellijSdk"] as String

repositories {
    maven { setUrl("$intellijRepo/$intellijReleaseType") }
}

val intellij by configurations.creating
val sources by configurations.creating
val `jps-standalone` by configurations.creating
val `jps-build-test` by configurations.creating
val `intellij-core` by configurations.creating

val customDepsRepoDir = File(buildDir, "repo")
val customDepsOrg: String by rootProject.extra
val customDepsRevision = intellijVersion
val customDepsRepoModulesDir = File(customDepsRepoDir, "$customDepsOrg/$customDepsRevision")
val repoDir = customDepsRepoModulesDir

dependencies {
    intellij("com.jetbrains.intellij.idea:$intellijSdkDependencyName:$intellijVersion")
    sources("com.jetbrains.intellij.idea:ideaIC:$intellijVersion:sources@jar")
    `jps-standalone`("com.jetbrains.intellij.idea:jps-standalone:$intellijVersion")
    `jps-build-test`("com.jetbrains.intellij.idea:jps-build-test:$intellijVersion")
    `intellij-core`("com.jetbrains.intellij.idea:intellij-core:$intellijVersion")
}

fun Task.configureExtractFromConfigurationTask(sourceConfig: Configuration, extractor: (Configuration) -> Any) {
    dependsOn(sourceConfig)
    inputs.files(sourceConfig)
    val targetDir = File(repoDir, sourceConfig.name)
    outputs.dirs(targetDir)
    doFirst {
        project.copy {
            from(extractor(sourceConfig))
            into(targetDir)
        }
    }
}

val unzipIntellijSdk by tasks.creating { configureExtractFromConfigurationTask(intellij) { zipTree(it.singleFile) } }

val unzipIntellijCore by tasks.creating { configureExtractFromConfigurationTask(`intellij-core`) { zipTree(it.singleFile) } }

val unzipJpsStandalone by tasks.creating { configureExtractFromConfigurationTask(`jps-standalone`) { zipTree(it.singleFile) } }

val copyIntellijSdkSources by tasks.creating { configureExtractFromConfigurationTask(sources) { it.singleFile } }

val copyJpsBuildTest by tasks.creating { configureExtractFromConfigurationTask(`jps-build-test`) { it.singleFile } }

fun writeIvyXml(moduleName: String, fileName: String, jarFiles: FileCollection, baseDir: File, sourcesJar: File) {
    with(IvyDescriptorFileGenerator(DefaultIvyPublicationIdentity(customDepsOrg, moduleName, intellijVersion))) {
        addConfiguration(DefaultIvyConfiguration("default"))
        addConfiguration(DefaultIvyConfiguration("sources"))
        jarFiles.asFileTree.files.forEach {
            if (it.isFile && it.extension == "jar") {
                val relativeName = it.toRelativeString(baseDir).removeSuffix(".jar")
                addArtifact(DefaultIvyArtifact(it, relativeName, "jar", "jar", null).also { it.conf = "default" })
            }
        }
        val sourcesArtifactName = sourcesJar.name.removeSuffix(".jar").substringBefore("-")
        addArtifact(DefaultIvyArtifact(sourcesJar, sourcesArtifactName, "jar", "sources", "sources").also { it.conf = "sources" })
        writeTo(File(customDepsRepoModulesDir, "$fileName.ivy.xml"))
    }
}

val prepareIvyXml by tasks.creating {
    dependsOn(unzipIntellijSdk, unzipIntellijCore, unzipJpsStandalone, copyIntellijSdkSources, copyJpsBuildTest)
    val intellijSdkDir = File(repoDir, intellij.name)
    inputs.dir(intellijSdkDir)
    outputs.file(File(repoDir, "${intellij.name}.ivy.xml"))
    val flatDeps = listOf(`intellij-core`, `jps-standalone`, `jps-build-test`)
    flatDeps.forEach {
        inputs.dir(File(repoDir, it.name))
        outputs.file(File(repoDir, "${it.name}.ivy.xml"))
    }
    inputs.dir(File(repoDir, sources.name))
    doFirst {
        val sourcesFile = File(repoDir, "${sources.name}/${sources.singleFile.name}")
        writeIvyXml(intellij.name, intellij.name,
                    files("$intellijSdkDir/lib/").filter { !it.name.startsWith("kotlin-") },
                    File(intellijSdkDir, "lib"),
                    sourcesFile)
        File(intellijSdkDir, "plugins").listFiles { it: File -> it.isDirectory }.forEach {
            writeIvyXml(it.name, "intellij.plugin.${it.name}", files("$it/lib/"), File(it, "lib"), sourcesFile)
        }
        flatDeps.forEach {
            writeIvyXml(it.name, it.name, files("$repoDir/${it.name}"), File(repoDir, it.name), sourcesFile)
        }
    }
}

val build by tasks.creating {
    dependsOn(prepareIvyXml)
}

val clean by tasks.creating(Delete::class) {
    delete(customDepsRepoModulesDir)
    delete(buildDir)
}
