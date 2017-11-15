
import java.io.File

// TODO: consider adding dx sources (the only jar used on the compile time so far)
// e.g. from "https://android.googlesource.com/platform/dalvik/+archive/android-5.0.0_r2/dx.tar.gz"

repositories {
    ivy {
        artifactPattern("https://dl-ssl.google.com/android/repository/[artifact]-[revision].[ext]")
        artifactPattern("https://dl-ssl.google.com/android/repository/[artifact]_[revision](-[classifier]).[ext]")
        artifactPattern("https://dl.google.com/android/repository/[artifact]_[revision](-[classifier]).[ext]")
    }
}

val androidSdk by configurations.creating
val androidJar by configurations.creating
val dxJar by configurations.creating
val androidPlatform by configurations.creating
val buildTools by configurations.creating

val libsDestDir = File(buildDir, "libs")
val sdkDestDir = File(buildDir, "androidSdk")

data class LocMap(val name: String, val ver: String, val dest: String, val suffix: String,
                  val additionalConfig: Configuration? = null, val dirLevelsToSkit: Int = 0)

val sdkLocMaps = listOf(
        LocMap("platform", "26_r02", "platforms/android-26", "", androidPlatform, 1),
        LocMap("android_m2repository", "r44", "extras/android", ""),
        LocMap("platform-tools", "r25.0.3", "", "linux"),
        LocMap("tools", "r24.3.4", "", "linux"),
        LocMap("build-tools", "r23.0.1", "build-tools/23.0.1", "linux", buildTools, 1))


val prepareSdk by task<DefaultTask> {
    outputs.dir(sdkDestDir)
    doLast {}
}

fun LocMap.toDependency(): String =
        "google:$name:$ver${suffix?.takeIf{ it.isNotEmpty() }?.let { ":$it" } ?: ""}@zip"

sdkLocMaps.forEach {
    val id = "${it.name}_${it.ver}"
    val cfg = configurations.create(id)
    val dependency = it.toDependency()
    dependencies.add(cfg.name, dependency)

    val t = task<Copy>("unzip_$id") {
        afterEvaluate {
            from(zipTree(cfg.singleFile))
        }
        into(file("$sdkDestDir/${it.dest}"))
    }
    if (it.dirLevelsToSkit > 0) {
        t.apply {
            eachFile {
                path = path.split("/").drop(it.dirLevelsToSkit).joinToString("/")
            }
        }
    }
    prepareSdk.dependsOn(t)

    it.additionalConfig?.also {
        dependencies.add(it.name, dependency)
    }
}

val clean by task<Delete> {
    delete(buildDir)
}

val extractAndroidJar by task<Copy> {
    configurations.add(androidJar)
    afterEvaluate {
        from(zipTree(androidPlatform.singleFile).matching { include("**/android.jar") }.files.first())
    }
    into(libsDestDir)
}

val extractDxJar by task<Copy> {
    configurations.add(dxJar)
    afterEvaluate {
        from(zipTree(buildTools.singleFile).matching { include("**/dx.jar") }.files.first())
    }
    into(libsDestDir)
}

artifacts.add(androidSdk.name, file("$sdkDestDir")) {
    builtBy(prepareSdk)
}

artifacts.add(androidJar.name, file("$libsDestDir/android.jar")) {
    builtBy(extractAndroidJar)
}

artifacts.add(dxJar.name, file("$libsDestDir/dx.jar")) {
    builtBy(extractDxJar)
}
