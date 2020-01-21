plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val addCidrDeps: (Project) -> Unit by ultimateTools

addCidrDeps(project)

dependencies {
    compile(kotlin("stdlib"))
    compile(project(":idea")) { isTransitive = false }
    compile(project(":idea:idea-gradle")) { isTransitive = false }
    compile(project(":kotlin-ultimate:ide:common-native"))
    compile(project(":kotlin-ultimate:ide:common-noncidr-native"))
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
}


the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

val jarTask = (tasks.findByName("jar") as Jar? ?: task<Jar>("jar")).apply {
    val classes = files(Callable {
        val result = files()
        val ideaGradle = project(":idea:idea-gradle").tasks.getByName("jar")
        val commonNative = project(":kotlin-ultimate:ide:common-native").tasks.getByName("jar")

        result.from(zipTree(
            ideaGradle.outputs.files.singleFile
        ).matching { include("org/**") })

        result.from(zipTree(
            commonNative.outputs.files.singleFile
        ))

        result.builtBy(ideaGradle, commonNative)
    })

    from(classes)
    archiveFileName.set("mobile-mpp.jar")
}

