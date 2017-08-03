
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":kotlin-stdlib"))
    buildVersion()
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

val jar: Jar by tasks
jar.apply {
    setupRuntimeJar("Kotlin Runner")
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.runner.Main")
    manifest.attributes.put("Class-Path", "kotlin-runtime.jar")
    archiveName = "kotlin-runner.jar"
}

dist {
    from(jar)
}

