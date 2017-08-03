
import java.io.File
import proguard.gradle.ProGuardTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.publication.maven.internal.deployer.MavenRemoteRepository

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
        classpath("net.sf.proguard:proguard-gradle:5.3.1")
    }
}

apply { plugin("kotlin") }

plugins {
    maven
}
//apply { plugin("maven") }


// Set to false to disable proguard run on kotlin-compiler.jar. Speeds up the build
val shrink = true

val compilerManifestClassPath =
        "kotlin-runtime.jar kotlin-reflect.jar kotlin-script-runtime.jar"

val fatJarContents by configurations.creating
val proguardLibraryJars by configurations.creating
val fatJar by configurations.creating
val compilerJar by configurations.creating
val embeddableCompilerJar by configurations.creating

val compilerBaseName: String by rootProject.extra
val embeddableCompilerBaseName: String by rootProject.extra

val javaHome = File(System.getProperty("java.home"))

val buildLocalRepoPath: File by rootProject.extra

val compilerModules: Array<String> by rootProject.extra
val otherCompilerModules = compilerModules.filter { it != path }

val kotlinEmbeddableRootPackage = "org.jetbrains.kotlin"

val packagesToRelocate =
        listOf("com.intellij",
               "com.google",
               "com.sampullara",
               "org.apache",
               "org.jdom",
               "org.picocontainer",
               "jline",
               "gnu",
               "javax.inject",
               "org.fusesource")

fun firstFromJavaHomeThatExists(vararg paths: String): File =
        paths.mapNotNull { File(javaHome, it).takeIf { it.exists() } }.firstOrNull()
        ?: throw GradleException("Cannot find under '$javaHome' neither of: ${paths.joinToString()}")

dependencies {
    val compile by configurations
    val compileOnly by configurations
    val testCompile by configurations
    val testCompileOnly by configurations
    val testRuntime by configurations
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:daemon-common"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(project(":build-common"))
    compileOnly(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
    compileOnly(commonDep("org.fusesource.jansi", "jansi"))
    compileOnly(commonDep("org.jline", "jline"))

    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(project(":compiler.tests-common"))
    testCompileOnly(project(":compiler:ir.ir2cfg"))
    testCompileOnly(project(":compiler:ir.tree")) // used for deepCopyWithSymbols call that is removed by proguard from the compiler TODO: make it more straightforward
    testCompile(ideaSdkDeps("openapi", "idea", "util", "asm-all", "commons-httpclient-3.1-patched"))
    // deps below are test runtime deps, but made test compile to split compilation and running to reduce mem req
    testCompile(project(":kotlin-stdlib"))
    testCompile(project(":kotlin-script-runtime"))
    testCompile(project(":kotlin-runtime"))
    testCompile(project(":kotlin-reflect"))
    testCompile(project(":plugins:android-extensions-compiler"))
    testCompile(project(":ant"))
    otherCompilerModules.forEach {
        testCompile(project(it))
        fatJarContents(project(it)) { isTransitive = false }
    }
    testRuntime(ideaSdkCoreDeps("*.jar"))
    testRuntime(ideaSdkDeps("*.jar"))
//    testRuntime(project(":prepare:compiler", configuration = "default"))

    buildVersion()

    fatJarContents(project(":core:builtins", configuration = "builtins"))
    fatJarContents(ideaSdkCoreDeps(*(rootProject.extra["ideaCoreSdkJars"] as Array<String>)))
    fatJarContents(ideaSdkDeps("jna-platform", "oromatcher"))
    fatJarContents(ideaSdkDeps("jps-model.jar", subdir = "jps"))
    fatJarContents(commonDep("javax.inject"))
    fatJarContents(commonDep("org.jline", "jline"))
    fatJarContents(protobufFull())
    fatJarContents(commonDep("com.github.spullara.cli-parser", "cli-parser"))
    fatJarContents(commonDep("com.google.code.findbugs", "jsr305"))
    fatJarContents(commonDep("io.javaslang", "javaslang"))
    fatJarContents(preloadedDeps("json-org"))

    proguardLibraryJars(files(firstFromJavaHomeThatExists("lib/rt.jar", "../Classes/classes.jar"),
                              firstFromJavaHomeThatExists("lib/jsse.jar", "../Classes/jsse.jar"),
                              firstFromJavaHomeThatExists("../lib/tools.jar", "../Classes/tools.jar")))
    proguardLibraryJars(project(":kotlin-stdlib", configuration = "mainJar"))
    proguardLibraryJars(project(":kotlin-script-runtime", configuration = "mainJar"))
    proguardLibraryJars(project(":kotlin-reflect", configuration = "mainJar"))
    proguardLibraryJars(preloadedDeps("kotlinx-coroutines-core"))

//    proguardLibraryJars(project(":prepare:runtime", configuration = "default").apply { isTransitive = false })
//    proguardLibraryJars(project(":prepare:reflect", configuration = "default").apply { isTransitive = false })
//    proguardLibraryJars(project(":core:script.runtime").apply { isTransitive = false })
}

configureKotlinProjectSources(
        "compiler/daemon/src",
        "compiler/conditional-preprocessor/src",
        sourcesBaseDir = rootDir)
configureKotlinProjectResources("idea/src", sourcesBaseDir = rootDir) {
    include("META-INF/extensions/common.xml",
            "META-INF/extensions/kotlin2jvm.xml",
            "META-INF/extensions/kotlin2js.xml")
}
configureKotlinProjectTests("tests")

testsJar {}

tasks.withType<Test> {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    dependsOn(":prepare:mock-runtime-for-test:dist")
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    systemProperty("kotlin.test.script.classpath", the<JavaPluginConvention>().sourceSets.getByName("test").output.classesDirs.joinToString(File.pathSeparator))
    jvmArgs("-ea", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx1200m", "-XX:+UseCodeCacheFlushing", "-XX:ReservedCodeCacheSize=128m", "-Djna.nosys=true")
    maxHeapSize = "1200m"
    ignoreFailures = true
}

val packCompiler by task<ShadowJar> {
    configurations = listOf(fatJar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDir = File(buildDir, "libs")
    baseName = compilerBaseName
    classifier = "before-proguard"
    dependsOn(protobufFullTask)

    setupRuntimeJar("Kotlin Compiler")
    from(getCompiledClasses())
    from(fatJarContents)

    manifest.attributes.put("Class-Path", compilerManifestClassPath)
    manifest.attributes.put("Main-Class", "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
}

val proguard by task<ProGuardTask> {
    dependsOn(packCompiler)
    configuration("$rootDir/compiler/compiler.pro")

    val outputJar = File(buildDir, "libs", "$compilerBaseName-after-proguard.jar")

    inputs.files(packCompiler.outputs.files.singleFile)
    outputs.file(outputJar)

    // TODO: remove after dropping compatibility with ant build
    doFirst {
        System.setProperty("kotlin-compiler-jar-before-shrink", packCompiler.outputs.files.singleFile.canonicalPath)
        System.setProperty("kotlin-compiler-jar", outputJar.canonicalPath)
    }

    libraryjars(proguardLibraryJars)
    printconfiguration("$buildDir/compiler.pro.dump")
}

val embeddable by task<ShadowJar> {
    destinationDir = File(buildDir, "libs")
    baseName = embeddableCompilerBaseName
    configurations = listOf(embeddableCompilerJar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(proguard)
    from(proguard)
    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf")
    packagesToRelocate.forEach {
        relocate(it, "$kotlinEmbeddableRootPackage.$it")
    }
    relocate("org.fusesource", "$kotlinEmbeddableRootPackage.org.fusesource") {
        // TODO: remove "it." after #KT-12848 get addressed
        exclude("org.fusesource.jansi.internal.CLibrary")
    }
}

dist {
    if (shrink) {
        from(proguard)
    } else {
        from(packCompiler)
    }
    rename(".*", compilerBaseName + ".jar")
}

artifacts.add(compilerJar.name, proguard.outputs.files.singleFile) {
    builtBy(proguard)
    classifier = ""
    name = compilerBaseName
}
artifacts.add(embeddableCompilerJar.name, embeddable.outputs.files.singleFile) {
    builtBy(embeddable)
    classifier = ""
    name = embeddableCompilerBaseName
}

