
import org.gradle.jvm.tasks.Jar

description = "Compiler runner + daemon client"

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-build-common"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-preloader"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:daemon-common"))
    compile(project(":kotlin-daemon-client"))
    compileOnly(project(":compiler:util"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    runtimeOnly(projectRuntimeJar(":kotlin-compiler-embeddable"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar: Jar by tasks
jar.apply {
    from(getSourceSetsFrom(":kotlin-daemon-client")["main"].output.classesDirs)
    from(getSourceSetsFrom(":compiler:daemon-common")["main"].output.classesDirs)
}

runtimeJar(rewriteDepsToShadedCompiler(jar))
sourcesJar()
javadocJar()

publish()
