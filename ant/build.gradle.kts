
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(commonDep("org.apache.ant", "ant"))
    compile(project(":compiler:preloader"))
    compile(project(":kotlin-stdlib"))
    buildVersion()
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

val jar: Jar by tasks
jar.apply {
    setupRuntimeJar("Kotlin Ant Tools")
    archiveName = "kotlin-ant.jar"
    from("$projectDir/src") { include("**/*.xml") }

    manifest.attributes.put("Class-Path", "kotlin-stdlib.jar kotlin-reflect.jar kotlin-script-runtime.jar kotlin-preloader.jar")
}

dist {
    from(jar)
}

