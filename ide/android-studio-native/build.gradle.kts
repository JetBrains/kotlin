plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val proprietaryRepositories: Project.() -> Unit by ultimateTools
val cidrVersion: String by rootProject.extra

proprietaryRepositories(project)

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    compileOnly(project(":compiler:util")) { isTransitive = false }
    compileOnly(project(":idea")) { isTransitive = false }
    compileOnly(project(":idea:idea-gradle")) { isTransitive = false }
    compileOnly(project(":idea:idea-core")) { isTransitive = false }
    compileOnly(intellijDep()) { includeJars(
        "external-system-rt",
        "extensions",
        "idea",
        "jdom",
        "platform-api",
        "platform-impl",
        "platform-util-ex",
        "platform-util-ui",
        "platform-ide-util-io",
        "util"
    ) }
    compileOnly(intellijPluginDep("android")) { includeJars("sdk-tools") }
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijPluginDep("java")) { includeJars(
        "java-api",
        "java-impl"
    ) }

    compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa-common:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-xcode-model-core:$cidrVersion") { isTransitive = false }
    api(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    api(project(":kotlin-ultimate:ide:common-noncidr-native")) { isTransitive = false }

    testImplementation(kotlin("stdlib"))
    testImplementation(project(":idea")) { isTransitive = false }
    testImplementation(commonDep("junit:junit"))
    testImplementation(intellijDep()) { includeJars(
        "platform-api",
        "util"
    ) }
}


the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

val jarTask = (tasks.findByName("jar") as Jar? ?: task<Jar>("jar")).apply {
    val classes = files(Callable {
        val result = files()
        val commonNative = project(":kotlin-ultimate:ide:common-native").tasks.getByName("jar")
        val noncidrNative = project(":kotlin-ultimate:ide:common-noncidr-native").tasks.getByName("jar")

        result.from(zipTree(
            commonNative.outputs.files.singleFile
        ))

        result.from(zipTree(
            noncidrNative.outputs.files.singleFile
        ))

        result.builtBy(noncidrNative)
    })

    from(classes)
    archiveFileName.set("mobile-mpp.jar")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
