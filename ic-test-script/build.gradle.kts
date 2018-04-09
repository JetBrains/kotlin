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
val vcsStartVersion = (findProperty("first.build.from.revision") as? String)?.takeIf { it.isNotBlank() }

fun fetchChanges(): List<Change>? {
    return buildId?.let {
        TeamCityInstanceFactory
                .guestAuth("https://teamcity.jetbrains.com")
                .build(BuildId(buildId))
                .fetchChanges()
    }
}


val tcChanges by lazy { fetchChanges() }

fun runGit(vararg args: String) {
    val process = ProcessBuilder()
            .command("git", *args)
            .directory(File("../clean"))
            .inheritIO()
            .start()
    if (process.waitFor() != 0) {
        throw GradleException("git ${args.joinToString(" ")} exited with non-zero code")
    }
}

fun configureRunKotlinBuild(l: GradleBuild.() -> Unit) = tasks.creating(GradleBuild::class) {
    l()
    buildFile = file("$dir/build.gradle.kts")
    tasks = listOf("dist", "classes", "testClasses")
}

val checkoutIncrementalBeforeChanges by tasks.creating {
    doFirst {

        val version = vcsStartVersion ?: tcChanges?.lastOrNull()?.version ?: vcsTargetVersion
        println("Worktree Checkout $version^")

        runGit("worktree", "add", "../incremental")

        runGit("checkout", "$version^")

        Unit
    }
}

val downloadCC by tasks.creating(Download::class) {
    src("https://github.com/ignatov/cc/releases/download/0.1/cc-all.jar")
    dest(buildDir)
}

val buildClassesInClean by configureRunKotlinBuild {
    dir = file("../clean")
}

val buildClassesInIncrementalFirst by configureRunKotlinBuild {
    dir = file("../incremental")
}

val checkoutIncrementalOriginal by tasks.creating {

    dependsOn(buildClassesInIncrementalFirst)
    doFirst {
        println("Checkout $vcsTargetVersion")

        runGit("checkout", "$vcsTargetVersion")

        Unit
    }
}

val buildClassesInIncrementalSecond by configureRunKotlinBuild {
    dir = file("../incremental")
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

val cleanupOnSuccess by tasks.creating(Delete::class) {
    delete("build/orig.zip")
    delete("build/inc.zip")
}