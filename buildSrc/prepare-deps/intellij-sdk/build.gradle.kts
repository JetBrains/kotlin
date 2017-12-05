
import org.gradle.api.publish.ivy.internal.artifact.DefaultIvyArtifact
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyConfiguration
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyPublicationIdentity
import org.gradle.api.publish.ivy.internal.publisher.IvyDescriptorFileGenerator
import java.io.File
import org.gradle.internal.os.OperatingSystem

val intellijRepo = "https://www.jetbrains.com/intellij-repository"
val intellijReleaseType = "releases" // or "snapshots"
val intellijSdkDependencyName = "ideaIC" // or "ideaIU"
val intellijVersion = rootProject.extra["versions.intellij"] as String

repositories {
    maven { setUrl("$intellijRepo/$intellijReleaseType") }
}

val intellijSdk by configurations.creating
val intellijSources by configurations.creating
val jpsStandalone by configurations.creating
val jpsBuildTest by configurations.creating
val intellijCore by configurations.creating

val repoDir = File(buildDir, "repo")

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

fun createIvyDependency(baseDir: File, file: File, configuration: String, extension: String?, type: String) : DefaultIvyArtifact {
    val relativePath = baseDir.toURI().relativize(file.toURI()).path
    val name = if (extension != null) relativePath.removeSuffix(".$extension") else relativePath
    val artifact = DefaultIvyArtifact(file, name, extension, type, null)
    artifact.conf = configuration
    return artifact
}

fun makeIvyXml(name: String, ivyFile: File, jarFiles: FileCollection, baseDir: File, sourcesJar: File): File {
    val generator = IvyDescriptorFileGenerator(DefaultIvyPublicationIdentity("com.jetbrains", name, intellijVersion))
    generator.addConfiguration(DefaultIvyConfiguration("default"))
    generator.addConfiguration(DefaultIvyConfiguration("compile"))
    generator.addConfiguration(DefaultIvyConfiguration("sources"))
    jarFiles.forEach {
        generator.addArtifact(createIvyDependency(baseDir, it, "compile", "jar", "jar"))
    }
    val relativeSourcesPath = baseDir.toURI().relativize(sourcesJar.parentFile.toURI()).path
    val sourcesArtifactName = sourcesJar.name.removeSuffix(".jar").substringBefore("-")
    val artifact = DefaultIvyArtifact(sourcesJar, "$relativeSourcesPath/$sourcesArtifactName", "jar", "sources", "sources")
    artifact.conf = "sources"
    generator.addArtifact(artifact)
    generator.writeTo(ivyFile)
    return ivyFile
}

val prepareIvyXml by tasks.creating {
    dependsOn(unzipIntellijSdk, unzipIntellijCore, unzipJpsStandalone, copyIntellijSdkSources, copyJpsBuildTest)
    inputs.dir(File(repoDir, intellijSdk.name))
    val flatDeps = listOf(intellijCore, jpsStandalone, jpsBuildTest)
    flatDeps.forEach {
        inputs.dir(File(repoDir, it.name))
    }
    inputs.dir(File(repoDir, intellijSources.name))
    val ivyXml = File(repoDir, "ivy.xml")
    outputs.files(ivyXml)
    val sourcesFile = File(repoDir, "${intellijSources.name}/${intellijSources.singleFile.name}")
    doFirst {
        makeIvyXml(intellijSdk.name, File(repoDir, "${intellijSdk.name}.ivy.xml"),
                   files("$repoDir/${intellijSdk.name}/lib").filter { !it.name.startsWith("kotlin-") },
                   repoDir, sourcesFile)
        File(repoDir, "${intellijSdk.name}/plugins").listFiles { it: File -> it.isDirectory }.forEach {
            makeIvyXml(it.name, File(repoDir, "intellij.plugin.${it.name}.ivy.xml"), files("$it/lib"), repoDir, sourcesFile)
        }
        flatDeps.forEach {
            makeIvyXml(it.name, File(repoDir, "${it.name}.ivy.xml"), files("$repoDir/${it.name}"), repoDir, sourcesFile)
        }
    }
}