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

fun updateTextInFile(file: File, regex: Regex, insertion: String) {
    check(file.isFile) { "$file is not found" }
    val text = file.readText()
    val match = regex.find(text) ?: error("Pattern(${regex.pattern}) is not found in file $file")
    file.writeText(text.replaceRange(match.range, insertion))
}

val writePluginVersion by tasks.creating {
    val projectDir = project(":kotlin-ultimate:ide:android-studio-native").projectDir
    val versionFile = projectDir.resolve("src/com/jetbrains/kmm/versions/VersionsUtils.kt")
    val pluginXmlFiles = projectDir.resolve("resources/META-INF").listFiles { f ->
        f.name.startsWith("plugin.xml")
    } ?: emptyArray()

    inputs.property("version", kmmPluginVersion)
    inputs.property("kotlinVersion", kotlinPluginVersion)
    outputs.file(versionFile)
    outputs.files(pluginXmlFiles)

    updateTextInFile(
        versionFile,
        Regex("const val pluginVersion: String = \"([^\"]+)\""),
        "const val pluginVersion: String = \"$kmmPluginVersion\""
    )
    updateTextInFile(
        versionFile,
        Regex("const val compiledAgainstKotlin: String = \"([^\"]+)\""),
        "const val compiledAgainstKotlin: String = \"$kotlinPluginVersion\""
    )

    val kmmPluginVersionNumber = kmmPluginVersion.split("-").first()
    pluginXmlFiles.forEach { f ->
        updateTextInFile(
            f,
            Regex("This version requires Kotlin Plugin: .+"),
            "This version requires Kotlin Plugin: $kotlinPluginVersion"
        )
        updateTextInFile(
            f,
            Regex("<version>.+</version>"),
            "<version>$kmmPluginVersion</version>"
        )
        updateTextInFile(
            f,
            Regex("Learn more about KMM: <a href=\".+\">"),
            "Learn more about KMM: <a href=\"https://kotlinlang.org/lp/mobile/?utm_medium=link&utm_source=product&utm_campaign=ASKMMI&utm_content=$kmmPluginVersionNumber\">"
        )
    }
}

tasks["compileKotlin"].dependsOn(writePluginVersion)
