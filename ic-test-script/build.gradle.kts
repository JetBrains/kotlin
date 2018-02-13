import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Task
import org.gradle.script.lang.kotlin.*
import org.jetbrains.teamcity.rest.*
import java.io.File

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath("org.jetbrains.teamcity:teamcity-rest-client:0.1.91")
    }
}
plugins {
    id("de.undercouch.download").version("1.2")
}

val buildId = findProperty("teamcity.build.id") as? String
val vcsTargetVersion = findProperty("build.vcs.number") as? String

val changes = buildId?.let {
    TeamCityInstanceFactory
            .guestAuth("https://teamcity.jetbrains.com")
            .build(BuildId(buildId))
            .fetchChanges()
}


val checkoutIncrementalBeforeChanges by tasks.creating {
    doFirst {

        val version = changes?.lastOrNull()?.version ?: vcsTargetVersion
        println("Worktree Checkout $version")

        val worktreeProcess = ProcessBuilder()
                .command("git", "worktree", "add", "../incremental")
                .directory(File("../clean"))
                .inheritIO()
                .start()
        if (worktreeProcess.waitFor() != 0) {
            throw GradleException("Git worktree exited with non-zero code")
        }

        val checkoutProcess = ProcessBuilder()
                .command("git", "checkout", "$version^")
                .directory(File("../clean"))
                .inheritIO()
                .start()
        if (checkoutProcess.waitFor() != 0) {
            throw GradleException("Git checkout exited with non-zero code")
        }

        Unit
    }
}

val downloadCC by tasks.creating(Download::class) {
    src("https://github.com/ignatov/cc/releases/download/0.1/cc-all.jar")
    dest(buildDir)
}

val buildClassesInClean by tasks.creating(GradleBuild::class) {
    dir = file("../clean")
    buildFile = file("$dir/build.gradle.kts")
    tasks = listOf("dist", "classes", "testClasses")
}

val buildClassesInIncrementalFirst by tasks.creating(GradleBuild::class) {
    dir = file("../incremental")
    buildFile = file("$dir/build.gradle.kts")
    tasks = listOf("dist", "classes", "testClasses")
}

val checkoutIncrementalOriginal by tasks.creating {

    dependsOn(buildClassesInIncrementalFirst)
    doFirst {
        println("Checkout $vcsTargetVersion")

        val process = ProcessBuilder()
                .command("git", "checkout", "$vcsTargetVersion")
                .directory(File("../incremental"))
                .inheritIO()
                .start()
        if (process.waitFor() != 0) {
            throw GradleException("Git exited with non-zero code")
        }

        Unit
    }
}

val buildClassesInIncrementalSecond by tasks.creating(GradleBuild::class) {
    dir = file("../incremental")
    buildFile = file("$dir/build.gradle.kts")
    tasks = listOf("dist", "classes", "testClasses")
    dependsOn(checkoutIncrementalOriginal)
}

val zipFromClean by tasks.creating(Zip::class) {
    from(fileTree("../clean/").matching {
        include("**/*.class")
    })
    destinationDir = buildDir
    archiveName = "orig.zip"

    isZip64 = true
    dependsOn(buildClassesInClean)
}

val zipFromIncremental by tasks.creating(Zip::class) {
    from(fileTree("../incremental/").matching {
        include("**/*.class")
    })
    destinationDir = buildDir
    archiveName = "inc.zip"

    isZip64 = true
    dependsOn(buildClassesInIncrementalSecond)
}

val unzipFromIncremental by tasks.creating(Copy::class) {
    dependsOn(zipFromIncremental)

    from(zipTree(zipFromIncremental.outputs.files.singleFile))
    into("$buildDir/inc")
}

val unzipFromClean by tasks.creating(Copy::class) {
    dependsOn(zipFromClean)

    from(zipTree(zipFromClean.outputs.files.singleFile))
    into("$buildDir/orig")
}

val runCompare by tasks.creating(JavaExec::class) {
    workingDir = buildDir
    maxHeapSize = "4g"
    args("-Dcompare.methods=false")
    classpath("$buildDir/cc-all.jar")
    main = "cc.CcKt"
    dependsOn(unzipFromClean, unzipFromIncremental)
}

val prepare by tasks.creating {
    dependsOn(downloadCC, checkoutIncrementalBeforeChanges)
}