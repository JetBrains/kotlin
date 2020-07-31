plugins {
    kotlin("jvm")
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val proprietaryRepositories: Project.() -> Unit by ultimateTools
val cidrVersion: String by rootProject.extra

val kotlinPluginVersion = (findProperty("pluginVersion")?.toString() ?: rootProject.extra["kotlinVersion"]) as String
val kmmPluginVersion: String = findProperty("kmmPluginVersion")?.toString() ?: "0.1-SNAPSHOT"

proprietaryRepositories(project)

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    compileOnly(project(":compiler:util")) { isTransitive = false }
    compileOnly(project(":idea")) { isTransitive = false }
    compileOnly(project(":idea:idea-frontend-independent"))
    compileOnly(project(":idea:idea-gradle")) { isTransitive = false }
    compileOnly(project(":idea:idea-core")) { isTransitive = false }
    compileOnly(intellijDep()) { includeJars(
        "external-system-rt",
        "extensions",
        "idea",
        "jdom",
        "libstudio.proto",
        "platform-api",
        "platform-impl",
        "platform-util-ex",
        "platform-core-ui",
        "platform-util-ui",
        "platform-ide-util-io",
        "protobuf-java-3.5.1",
        "util"
    ) }
    compileOnly(intellijPluginDep("android")) { includeJars(
        "adt-ui",
        "android",
        "android-common",
        "sdklib",
        "sdk-tools",
        "wizard-template"
    ) }
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijPluginDep("properties"))
    compileOnly(intellijPluginDep("java")) { includeJars(
        "java-api",
        "java-impl"
    ) }

    compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa-common:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-cocoa:$cidrVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.cidr:cidr-xcode-model-core:$cidrVersion") { isTransitive = false }

    implementation(project(":libraries:tools:new-project-wizard"))
    api(project(":kotlin-ultimate:ide:common-native")) { isTransitive = false }
    api(project(":kotlin-ultimate:ide:common-noncidr-native")) { isTransitive = false }
    api(project(":kotlin-ultimate:ide:common-cidr-mobile")) { isTransitive = false }

    testImplementation(project(":idea:idea-new-project-wizard"))
    testImplementation(kotlin("stdlib"))
    testImplementation(commonDep("junit:junit"))
    testImplementation(intellijDep())
    testImplementation(intellijPluginDep("android"))
    testImplementation(intellijPluginDep("java"))
    testImplementation(intellijPluginDep("gradle"))

    testImplementation(projectTests(":idea"))
    testImplementation(projectTests(":idea:idea-gradle"))
    testImplementation(projectTests(":idea:idea-test-framework"))
    testImplementation(projectTests(":idea:idea-new-project-wizard"))
    testImplementation(projectTests(":libraries:tools:new-project-wizard:new-project-wizard-cli"))

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(project(":allopen-ide-plugin"))
    testRuntimeOnly(project(":kotlin-gradle-statistics"))
    testRuntimeOnly(project(":kotlin-scripting-idea"))
    testRuntimeOnly(project(":kotlinx-serialization-ide-plugin"))
    testRuntimeOnly(project(":noarg-ide-plugin"))
    testRuntimeOnly(project(":plugins:kapt3-idea"))
    testRuntimeOnly(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    testRuntimeOnly(project(":sam-with-receiver-ide-plugin"))
}


the<JavaPluginConvention>().sourceSets["main"].apply {
    java.setSrcDirs(listOf("src"))
    resources.setSrcDirs(listOf("resources"))
}

val jarTask = (tasks.findByName("jar") as Jar? ?: task<Jar>("jar")).apply {
    val classes = files(Callable {
        val result = files()
        val wizardLib = project(":libraries:tools:new-project-wizard").tasks.getByName("jar")
        val commonNative = project(":kotlin-ultimate:ide:common-native").tasks.getByName("jar")
        val noncidrNative = project(":kotlin-ultimate:ide:common-noncidr-native").tasks.getByName("jar")
        val cidrMobile = project(":kotlin-ultimate:ide:common-cidr-mobile").tasks.getByName("jar")

        for (jar in listOf(wizardLib, commonNative, noncidrNative, cidrMobile)) {
            result.from(zipTree(
                jar.outputs.files.singleFile
            ))
        }

        result.builtBy(noncidrNative)
    })

    from(classes)
    archiveFileName.set("kmm.jar")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir.resolve("kotlin-ultimate/ide/android-studio-native/")
    useAndroidSdk()
}

fun replaceVersion(versionFile: File, versionPrefix: String, replacement: (MatchResult) -> String) {
    check(versionFile.isFile) { "Version file $versionFile is not found" }
    val text = versionFile.readText()
    val pattern = Regex("$versionPrefix\"([^\"]+)\"")
    val match = pattern.find(text) ?: error("Version pattern is missing in file $versionFile")
    val newValue = replacement(match)
    versionFile.writeText(text.replaceRange(match.groups[1]!!.range, newValue))
}

val writePluginVersion by tasks.creating {
    val versionFile = project(":kotlin-ultimate:ide:android-studio-native")
        .projectDir
        .resolve("src/com/jetbrains/kmm/versions/VersionsUtils.kt")

    inputs.property("version", kmmPluginVersion)
    inputs.property("kotlinVersion", kotlinPluginVersion)
    outputs.file(versionFile)

    replaceVersion(versionFile, "const val pluginVersion: String = ") {
        kmmPluginVersion
    }

    replaceVersion(versionFile, "const val compiledAgainstKotlin: String = ") {
        kotlinPluginVersion
    }
}

tasks["compileKotlin"].dependsOn(writePluginVersion)
