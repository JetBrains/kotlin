
import org.gradle.jvm.tasks.Jar

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":kotlin-stdlib"))
    compile(project(":compiler:cli-common"))
    compile(ideaSdkDeps("gradle-tooling-api",
                        "gradle-tooling-extension-api",
                        "gradle",
                        "gradle-core",
                        "gradle-base-services-groovy",
                        subdir = "plugins/gradle/lib"))
    buildVersion()
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()


val jar: Jar by tasks

ideaPlugin {
    from(jar)
}
