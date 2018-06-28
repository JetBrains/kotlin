import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Daemon Client New"

apply { plugin("kotlin") }

jvmTarget = "1.6"

val nativePlatformVariants: List<String> by rootProject.extra

val fatJarContents by configurations.creating

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:daemon-common-new"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(project(":kotlin-daemon-client"))
    fatJarContents(project(":kotlin-daemon-client"))
    compileOnly(commonDep("net.rubygrapefruit", "native-platform"))
    compileOnly(project(":compiler:daemon-common")) { isTransitive = false }
    compileOnly(project(":compiler:daemon-common-new")) { isTransitive = false }
    fatJarContents(commonDep("net.rubygrapefruit", "native-platform"))
    nativePlatformVariants.forEach {
        fatJarContents(commonDep("net.rubygrapefruit", "native-platform", "-$it"))
    }
    compileOnly(projectDist(":kotlin-reflect"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) { isTransitive = false }
    compile(commonDep("io.ktor", "ktor-network")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar(task<ShadowJar>("shadowJar")) {
    from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
    from(fatJarContents)
}

sourcesJar()
javadocJar()

dist()

ideaPlugin()

publish()
