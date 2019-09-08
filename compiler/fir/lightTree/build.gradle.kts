plugins {
    kotlin("jvm")
}

group = "org.jetbrains.kotlin.fir"
val jmhVersion = "1.21"
val testDataPath = "$rootDir/compiler/fir/lightTree/testData/coroutines"

repositories {
    mavenCentral()
    mavenLocal()
    maven { setUrl("https://www.jetbrains.com/intellij-repository/releases") }
    maven { setUrl("https://jetbrains.bintray.com/intellij-third-party-dependencies") }
}

dependencies {
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "guava", rootProject = rootProject) }
    compile(project(":compiler:psi"))
    
    compile("junit", "junit", "4.4")
    compile(projectTests(":compiler:fir:psi2fir"))

    compile("org.openjdk.jmh", "jmh-core", jmhVersion)
    compile("org.openjdk.jmh", "jmh-generator-bytecode", jmhVersion)
    compile("org.openjdk.jmh", "jmh-generator-annprocess", jmhVersion)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
    exclude("**/benchmark/**")
}

val compactClasspath by tasks.registering(Jar::class) {
    archiveAppendix.set("classpath")
    inputs.files(sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath)
    doFirst {
        manifest {
            attributes["Class-Path"] = (sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath).files
                .joinToString(separator = " ", transform = { it.toURI().toURL().toString() })
        }
    }
}

val jmhBytecode by tasks.registering(JavaExec::class) {
    tasks["classes"].mustRunAfter(tasks["clean"])
    tasks["compactClasspath"].mustRunAfter(tasks["classes"])
    dependsOn(tasks["clean"])
    dependsOn(tasks["classes"])
    dependsOn(tasks["compactClasspath"])

    classpath = files(tasks["compactClasspath"].outputs.files.singleFile.absolutePath)
    main = "org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator"
    args(
        "${project.buildDir}/classes/kotlin/test", "${project.buildDir}/generated-sources/jmh/",
        "${project.buildDir}/classes/kotlin/test", "default"
    )
}

tasks {
    compileTestJava {
        source(fileTree("${project.buildDir}/generated-sources/jmh/"))
        destinationDir = file("${project.buildDir}/generated-classes/jmh/")
    }
}

val jmhCompile by tasks.registering(JavaCompile::class) {
    /*classpath = sourceSets["test"].runtimeClasspath + files("${project.buildDir}/generated-sources/jmh/")

    source(fileTree("${project.buildDir}/generated-sources/jmh/"))
    destinationDir = file("${project.buildDir}/generated-classes/jmh/")*/
}

val jmhExec by tasks.registering(JavaExec::class) {
    dependsOn(tasks["compileTestJava"])
    doFirst {
        classpath = files(
            tasks["compactClasspath"].outputs.files.singleFile.absolutePath,
            "${project.buildDir}/generated-classes/jmh/",
            "${project.buildDir}/classes/kotlin/test"
        )
    }

    main = "org.openjdk.jmh.Main"

    workingDir = rootDir
    systemProperty("idea.home.path", project.intellijRootDir().absolutePath)
    systemProperty("idea.max.intellisense.filesize", 5000 * 1024)
    configurations.plusAssign(project.configurations["compile"])
}
