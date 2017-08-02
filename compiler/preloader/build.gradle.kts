
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(ideaSdkDeps("asm-all"))
    buildVersion()
}

configureKotlinProjectSources("src", "instrumentation/src")
configureKotlinProjectNoTests()

val jar: Jar by tasks
jar.apply {
    setupRuntimeJar("Kotlin Preloader")
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.preloading.Preloader")
    archiveName = "kotlin-preloader.jar"
}

dist {
    from(jar)
}

