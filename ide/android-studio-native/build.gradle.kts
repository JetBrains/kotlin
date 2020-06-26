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
    compileOnly(project(":idea:idea-frontend-independent"))
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
    compileOnly(intellijPluginDep("android")) { includeJars(
        "sdk-tools",
        "wizard-template"
    ) }
    compileOnly(intellijPluginDep("gradle"))
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

    testImplementation(kotlin("stdlib"))
    testImplementation(commonDep("junit:junit"))
    testImplementation(intellijDep())
    testImplementation(intellijPluginDep("android"))
    testImplementation(intellijPluginDep("java")) { includeJars(
        "java-api",
        "java-impl",
        "java_resources_en"
    ) }
    testImplementation(intellijPluginDep("gradle"))

    testImplementation(project(":idea:idea-new-project-wizard"))
    testImplementation(projectTests(":idea:idea-test-framework"))
    testImplementation(projectTests(":idea:idea-new-project-wizard"))
    testImplementation(projectTests(":libraries:tools:new-project-wizard:new-project-wizard-cli"))
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
    archiveFileName.set("mobile-mpp.jar")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir.resolve("kotlin-ultimate/ide/android-studio-native/")
    useAndroidSdk()
}

