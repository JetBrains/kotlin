
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

val nativePlatformUberjar = "$rootDir/dependencies/native-platform-uberjar.jar"

dependencies {
    val compile by configurations
    compile(project(":compiler:util"))
    compile(project(":compiler:cli-common"))
    compile(project(":compiler:daemon-common"))
    compile(files(nativePlatformUberjar))
    buildVersion()
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

val jar: Jar by tasks
jar.apply {
    setupRuntimeJar("Kotlin Daemon Client")
    from(zipTree(nativePlatformUberjar))
    archiveName = "kotlin-daemon-client.jar"
}

val sourcesJar by task<Jar> {
    setupSourceJar("Kotlin Daemon Client")
    archiveName = "kotlin-daemon-client-sources.jar"
}

dist {
    from(jar)
}

