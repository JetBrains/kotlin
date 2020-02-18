plugins {
    kotlin("jvm")
}

val clionVersion: String by rootProject.extra

repositories {
    maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addCidrDeps: (Project) -> Unit by ultimateTools

addCidrDeps(project)

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":idea")) { isTransitive = false }
    implementation(project(":idea:idea-gradle")) { isTransitive = false }
    implementation(project(":kotlin-ultimate:ide:common-native"))
    implementation(project(":kotlin-ultimate:ide:common-noncidr-native"))
    compileOnly(intellijDep()) { includeJars(
        "android-base-common",
        "external-system-rt",
        "extensions",
        "jdom",
        "platform-api",
        "platform-impl",
        "platform-util-ex",
        "platform-util-ui",
        "util"
    ) }
    compileOnly(intellijPluginDep("android")) { includeJars("sdk-tools") }
    compileOnly(intellijPluginDep("java")) { includeJars(
        "java-api",
        "java-impl"
    ) }

    implementation("com.jetbrains.intellij.cidr:cidr-cocoa-common:$clionVersion") { isTransitive = false }
    implementation("com.jetbrains.intellij.cidr:cidr-xcode-model-core:$clionVersion") { isTransitive = false }

    implementation(project(":idea:jvm-debugger:jvm-debugger-core")) { isTransitive = false }
}


the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

val jarTask = (tasks.findByName("jar") as Jar? ?: task<Jar>("jar")).apply {
    val classes = files(Callable {
        val result = files()
        val commonNative = project(":kotlin-ultimate:ide:common-native").tasks.getByName("jar")

        result.from(zipTree(
            commonNative.outputs.files.singleFile
        ))

        result.builtBy(commonNative)
    })

    from(classes)
    archiveFileName.set("mobile-mpp.jar")
}

