import org.gradle.kotlin.dsl.support.serviceOf

/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("project-tests-convention")
}

repositories {
    mavenLocal()
}

val composeCompilerPlugin by configurations.creating

dependencies {
    testApi(intellijCore())

    testRuntimeOnly(libs.xerces)
    testRuntimeOnly(commonDependency("org.apache.commons:commons-lang3"))

    testImplementation(libs.junit4)
    testCompileOnly(kotlinTest("junit"))
    testApi(testFixtures(project(":compiler:tests-common")))

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testApi(testFixtures(project(":compiler:fir:analysis-tests:legacy-fir-tests")))
    testApi(project(":compiler:fir:resolve"))
    testApi(project(":compiler:fir:providers"))
    testApi(project(":compiler:fir:semantics"))
    testApi(project(":compiler:fir:dump"))

    testRuntimeOnly(project(":compiler:fir:plugin-utils"))

    composeCompilerPlugin(project(":plugins:compose-compiler-plugin:compiler-hosted")) { isTransitive = false }

    val asyncProfilerClasspath = project.findProperty("fir.bench.async.profiler.classpath") as? String
    if (asyncProfilerClasspath != null) {
        testRuntimeOnly(files(*asyncProfilerClasspath.split(File.pathSeparatorChar).toTypedArray()))
    }



    embedded(intellijCore())
    embedded(libs.xerces)
    embedded(commonDependency("commons-lang:commons-lang"))

    embedded(libs.junit4)
    embedded(kotlinTest("junit"))
    embedded(projectTests(":compiler:tests-common"))

    embedded(project(":core:descriptors.runtime"))
    embedded(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    embedded(project(":compiler:fir:resolve"))
    embedded(project(":compiler:fir:providers"))
    embedded(project(":compiler:fir:semantics"))
    embedded(project(":compiler:fir:dump"))
    embedded(project(":compiler:fir:plugin-utils"))

}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

optInToK1Deprecation()

projectTests {
    val modelDumpAndReadTest = "org.jetbrains.kotlin.fir.ModelDumpAndReadTest"

    testTask(minHeapSizeMb = 8192, maxHeapSizeMb = 8192, reservedCodeCacheSizeMb = 512, jUnitMode = JUnitMode.JUnit4) {
        dependsOn(":dist", ":plugins:compose-compiler-plugin:compiler-hosted:jar")
        systemProperties(project.properties.filterKeys { it.startsWith("fir.") })
        workingDir = rootDir
        val composePluginClasspath = composeCompilerPlugin.asPath

        filter {
            excludeTestsMatching(modelDumpAndReadTest)
        }
        run {
            systemProperty("fir.bench.compose.plugin.classpath", composePluginClasspath)
            val argsExt = project.findProperty("fir.modularized.jvm.args") as? String
            if (argsExt != null) {
                val paramRegex = "([^\"]\\S*|\".+?\")\\s*".toRegex()
                jvmArgs(paramRegex.findAll(argsExt).map { it.groupValues[1] }.toList())
            }
        }
    }

    testTask("modelDumpTest", jUnitMode = JUnitMode.JUnit4, skipInLocalBuild = false) {
        dependsOn(":dist")
        workingDir = rootDir
        filter {
            includeTestsMatching(modelDumpAndReadTest)
        }
    }

    jvmArgs("-XX:CompileCommand=option,java.lang.*::*,DumpReplay")


}



fun org.gradle.jvm.tasks.Jar.addEmbeddedRuntime2(embeddedConfigurationName: String = "embedded") {
    project.configurations.findByName(embeddedConfigurationName)?.let { embedded ->
        dependsOn(embedded)
        val archiveOperations = project.serviceOf<ArchiveOperations>()
        from {
            embedded.map { dependency: File ->

                if (dependency.extension.equals("jar", ignoreCase = true)) {
                    archiveOperations.zipTree(dependency)
                } else {
                    dependency
                }
            }
        }
        manifest {
            attributes["Main-Class"] = "org.jetbrains.kotlin.fir.StandaloneModularizedTestRunner"
        }
    }
}


fun Project.runtimeJar2(body: org.gradle.jvm.tasks.Jar.() -> Unit = {}): TaskProvider<out org.gradle.jvm.tasks.Jar> {
    val jarTask = tasks.register<org.gradle.jvm.tasks.Jar>("someJar")
    jarTask.configure {
        addEmbeddedRuntime2("testRuntimeClasspath")
        setupPublicJar(project.extensions.getByType<BasePluginExtension>().archivesName.get())
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        pluginManager.withPlugin("java") {
            from(testSourceSet.output)
        }
        from(file("ddd"))
        this.archiveBaseName = "owo"
        body()
    }

    return jarTask
}

runtimeJar2()


testsJar()
