
import org.gradle.api.publish.ivy.internal.artifact.DefaultIvyArtifact
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyConfiguration
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyPublicationIdentity
import org.gradle.api.publish.ivy.internal.publisher.IvyDescriptorFileGenerator
import java.io.File
import org.gradle.internal.os.OperatingSystem

val intellijRepo = "https://www.jetbrains.com/intellij-repository"
val intellijReleaseType = "releases" // or "snapshots"
val intellijSdkDependencyName = "ideaIC" // or "ideaIU"
val intellijVersion = rootProject.extra["versions.intellijSdk"] as String

repositories {
    maven { setUrl("$intellijRepo/$intellijReleaseType") }
}

val intellijSdk by configurations.creating
val intellijSources by configurations.creating
val jpsStandalone by configurations.creating
val jpsBuildTest by configurations.creating
val intellijCore by configurations.creating

val customDepsRepoDir = File(buildDir, "repo")
val customDepsOrg: String by rootProject.extra
val customDepsRevision = intellijVersion
val customDepsRepoModulesDir = File(customDepsRepoDir, "$customDepsOrg/$customDepsRevision")
val repoDir = customDepsRepoModulesDir

dependencies {
    intellijSdk("com.jetbrains.intellij.idea:$intellijSdkDependencyName:$intellijVersion")
    intellijSources("com.jetbrains.intellij.idea:ideaIC:$intellijVersion:sources@jar")
    jpsStandalone("com.jetbrains.intellij.idea:jps-standalone:$intellijVersion")
    jpsBuildTest("com.jetbrains.intellij.idea:jps-build-test:$intellijVersion")
    intellijCore("com.jetbrains.intellij.idea:intellij-core:$intellijVersion")
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

val unzipIntellijSdk by tasks.creating { configureExtractFromConfigurationTask(intellijSdk) { zipTree(it.singleFile) } }

val unzipIntellijCore by tasks.creating { configureExtractFromConfigurationTask(intellijCore) { zipTree(it.singleFile) } }

val unzipJpsStandalone by tasks.creating { configureExtractFromConfigurationTask(jpsStandalone) { zipTree(it.singleFile) } }

val copyIntellijSdkSources by tasks.creating { configureExtractFromConfigurationTask(intellijSources) { it.singleFile } }

val copyJpsBuildTest by tasks.creating { configureExtractFromConfigurationTask(jpsBuildTest) { it.singleFile } }

fun writeIvyXml(moduleName: String, jarFiles: FileCollection, baseDir: File, sourcesJar: File) {
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
        writeTo(File(customDepsRepoModulesDir, "$moduleName.ivy.xml"))
    }
}

val prepareIvyXml by tasks.creating {
    dependsOn(unzipIntellijSdk, unzipIntellijCore, unzipJpsStandalone, copyIntellijSdkSources, copyJpsBuildTest)
    val intellijSdkDir = File(repoDir, intellijSdk.name)
    inputs.dir(intellijSdkDir)
    outputs.file(File(repoDir, "${intellijSdk.name}.ivy.xml"))
    val flatDeps = listOf(intellijCore, jpsStandalone, jpsBuildTest)
    flatDeps.forEach {
        inputs.dir(File(repoDir, it.name))
        outputs.file(File(repoDir, "${it.name}.ivy.xml"))
    }
    inputs.dir(File(repoDir, intellijSources.name))
//    outputs.files("$repoDir/intellij.plugin.*.ivy.xml")
    doFirst {
        val sourcesFile = File(repoDir, "${intellijSources.name}/${intellijSources.singleFile.name}")
        writeIvyXml(intellijSdk.name,
                    files("$intellijSdkDir/lib/").filter { !it.name.startsWith("kotlin-") },
                    File(intellijSdkDir, "lib"),
                    sourcesFile)
        File(intellijSdkDir, "plugins").listFiles { it: File -> it.isDirectory }.forEach {
            writeIvyXml("intellij.plugin.${it.name}", files("$it/lib/"), File(it, "lib"), sourcesFile)
        }
        flatDeps.forEach {
            writeIvyXml(it.name, files("$repoDir/${it.name}"), File(repoDir, it.name), sourcesFile)
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
