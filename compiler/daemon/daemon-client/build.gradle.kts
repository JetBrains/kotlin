import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Daemon Client"

apply { plugin("kotlin") }

jvmTarget = "1.6"

val nativePlatformVariants: List<String> by rootProject.extra

val fatJarContents by configurations.creating

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:daemon-common"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(commonDep("net.rubygrapefruit", "native-platform"))
    fatJarContents(project(":compiler:daemon-common")) { isTransitive = false }
    fatJarContents(commonDep("net.rubygrapefruit", "native-platform"))
    nativePlatformVariants.forEach {
        fatJarContents(commonDep("net.rubygrapefruit", "native-platform", "-$it"))
    }
    compile(projectDist(":kotlin-reflect"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) { isTransitive = false }
    compile(commonDep("io.ktor", "ktor-network")) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
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
