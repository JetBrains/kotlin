package org.jetbrains.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec

class NativeInteropPlugin implements Plugin<Project> {

    @Override
    void apply(Project prj) {
        def generatedSrcDir = new File(prj.buildDir, "nativeInteropStubs/kotlin")
        def nativeLibsDir = new File(prj.buildDir, "nativelibs")

        prj.configurations {
            interopStubGenerator
        }

        prj.dependencies {
            compile project(path: ':Interop:Runtime')
            interopStubGenerator project(path: ":Interop:StubGenerator")
        }

        def genStubsTaskName = "genInteropStubs"

        prj.task(genStubsTaskName, type: JavaExec) {
            classpath = prj.configurations.interopStubGenerator
            main = "org.jetbrains.kotlin.native.interop.gen.jvm.MainKt"
            args = [generatedSrcDir, nativeLibsDir]
            systemProperties "java.library.path" : new File(prj.findProject(":Interop:Indexer").buildDir, "nativelibs")
            systemProperties "llvmInstallPath" : prj.llvmInstallPath
            environment "LIBCLANG_DISABLE_CRASH_RECOVERY": "1"
            environment "DYLD_LIBRARY_PATH": "${prj.llvmInstallPath}/lib"

            outputs.dir generatedSrcDir
            outputs.dir nativeLibsDir
        }

        prj.afterEvaluate {
            prj.tasks.getByName(genStubsTaskName) {
                // TODO: handle other source sets
                prj.sourceSets.each {
                    it.kotlin.srcDirs.each { srcDir ->
                      inputs.files prj.fileTree(srcDir.path).include('**/*.def')
                      args srcDir
                    }
                }
            }
        }

        prj.sourceSets {
            main {
                kotlin {
                    srcDirs generatedSrcDir
                }
            }
        }

        prj.tasks.getByName("compileKotlin") {
            dependsOn genStubsTaskName
        }

        // FIXME: choose tasks more wisely
        prj.tasks.withType(JavaExec) {
            if (name != genStubsTaskName) {
                systemProperties "java.library.path": nativeLibsDir
            }
        }
    }
}
