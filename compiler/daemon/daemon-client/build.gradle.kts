import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Daemon Client"

apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

jvmTarget = "1.6"

val nativePlatformVariants: List<String> by rootProject.extra

containsEmbeddedComponents()

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:daemon-common"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(commonDep("net.rubygrapefruit", "native-platform"))

    embeddedComponents(project(":compiler:daemon-common")) { isTransitive = false }
    embeddedComponents(commonDep("net.rubygrapefruit", "native-platform"))
    nativePlatformVariants.forEach {
        embeddedComponents(commonDep("net.rubygrapefruit", "native-platform", "-$it"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

noDefaultJar()

runtimeJar(task<ShadowJar>("shadowJar")) {
    from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
    fromEmbeddedComponents()
}

sourcesJar()
javadocJar()

dist()

ideaPlugin()

publish()
