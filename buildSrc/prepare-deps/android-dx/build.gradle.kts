
import org.gradle.api.publish.ivy.internal.artifact.DefaultIvyArtifact
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyConfiguration
import org.gradle.api.publish.ivy.internal.publication.DefaultIvyPublicationIdentity
import org.gradle.api.publish.ivy.internal.publisher.IvyDescriptorFileGenerator
import java.io.File
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar

val toolsOs by lazy {
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

val buildToolsVersion = rootProject.extra["versions.androidBuildTools"] as String
val dxSourcesVersion = rootProject.extra["versions.androidDxSources"] as String

repositories {
    ivy {
        artifactPattern("https://dl-ssl.google.com/android/repository/[artifact]_[revision](-[classifier]).[ext]")
        artifactPattern("https://android.googlesource.com/platform/dalvik/+archive/android-$dxSourcesVersion/[artifact].[ext]")
    }
}

val customDepsRepoDir = File(buildDir, "repo")
val customDepsOrg: String by rootProject.extra
val dxModuleName = "android-dx"
val dxRevision = buildToolsVersion
val dxRepoModuleDir = File(customDepsRepoDir, "$customDepsOrg/$dxModuleName/$dxRevision")

val buildToolsZip by configurations.creating
val dxSourcesTar by configurations.creating

dependencies {
    buildToolsZip("google:build-tools:$buildToolsVersion:$toolsOs@zip")
    dxSourcesTar("google:dx:0@tar.gz")
}

val unzipDxJar by tasks.creating {
    dependsOn(buildToolsZip)
    inputs.files(buildToolsZip)
    outputs.files(File(dxRepoModuleDir, "dx.jar"))
    doFirst {
        project.copy {
            from(zipTree(buildToolsZip.singleFile).files)
            include("**/dx.jar")
            into(dxRepoModuleDir)
        }
    }
}

val dxSourcesTargetDir = File(buildDir, "dx_src")

val untarDxSources by tasks.creating {
    dependsOn(dxSourcesTar)
    inputs.files(dxSourcesTar)
    outputs.dir(dxSourcesTargetDir)
    doFirst {
        project.copy {
            from(tarTree(dxSourcesTar.singleFile))
            include("src/**")
            includeEmptyDirs = false
            into(dxSourcesTargetDir)
        }
    }
}

val prepareDxSourcesJar by tasks.creating(Jar::class) {
    dependsOn(untarDxSources)
    from("$dxSourcesTargetDir/src")
    destinationDir = dxRepoModuleDir
    baseName = "dx"
    classifier = "sources"
}

val prepareIvyXml by tasks.creating {
    dependsOn(unzipDxJar, prepareDxSourcesJar)
    inputs.files(unzipDxJar, prepareDxSourcesJar)
    val ivyFile = File(dxRepoModuleDir, "$dxModuleName.ivy.xml")
    outputs.file(ivyFile)
    doLast {
        with(IvyDescriptorFileGenerator(DefaultIvyPublicationIdentity(customDepsOrg, dxModuleName, dxRevision))) {
            addConfiguration(DefaultIvyConfiguration("default"))
            addConfiguration(DefaultIvyConfiguration("sources"))
            addArtifact(DefaultIvyArtifact(File(dxRepoModuleDir, "dx.jar"), "dx", "jar", "jar", null).also { it.conf = "default" })
            addArtifact(DefaultIvyArtifact(File(dxRepoModuleDir, "dx-sources.jar"), "dx", "jar", "sources", "sources").also { it.conf = "sources" })
            writeTo(ivyFile)
        }
    }
}

val build by tasks.creating {
    dependsOn(unzipDxJar, prepareDxSourcesJar, prepareIvyXml)
}

val clean by tasks.creating(Delete::class) {
    delete(dxRepoModuleDir)
    delete(buildDir)
}
