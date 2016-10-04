package org.jetbrains.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec

class NativeInteropPlugin implements Plugin<Project> {

    @Override
    void apply(Project prj) {
        def nativeLibsDir = new File(prj.buildDir, "nativelibs")

        prj.configurations {
            interopStubGenerator
        }

        prj.dependencies {
            interopStubGenerator project(path: ":Interop:StubGenerator")
        }

        prj.sourceSets.all { sourceSet ->
            def generatedSrcDir = new File(prj.buildDir, "nativeInteropStubs/${sourceSet.name}/kotlin")

            prj.dependencies {
                add sourceSet.getCompileConfigurationName(), project(path: ':Interop:Runtime')
            }

            def genStubsTask = prj.task(sourceSet.getTaskName("gen", "interopStubs"), type: JavaExec) {
                classpath = prj.configurations.interopStubGenerator
                main = "org.jetbrains.kotlin.native.interop.gen.jvm.MainKt"
                systemProperties "java.library.path" : new File(prj.findProject(":Interop:Indexer").buildDir, "nativelibs")
                systemProperties "llvmInstallPath" : prj.llvmInstallPath
                environment "LIBCLANG_DISABLE_CRASH_RECOVERY": "1"
                environment "DYLD_LIBRARY_PATH": "${prj.llvmInstallPath}/lib"

                outputs.dir generatedSrcDir
                outputs.dir nativeLibsDir

                args = [generatedSrcDir, nativeLibsDir]

                prj.afterEvaluate { // FIXME: it is a hack
                    sourceSet.kotlin.srcDirs.each { srcDir ->
                        if (srcDir != generatedSrcDir) {
                            args srcDir
                            inputs.files prj.fileTree(srcDir.path).include('**/*.def')
                        }
                    }
                }
            }

            sourceSet.kotlin.srcDirs generatedSrcDir

            prj.tasks.getByName(sourceSet.getTaskName("compile", "Kotlin")) {
                dependsOn genStubsTask
            }
        }

        // FIXME: choose tasks more wisely
        prj.tasks.withType(JavaExec) {
            if (!name.endsWith("InteropStubs")) {
                systemProperties "java.library.path": nativeLibsDir
            }
        }
    }
}
