package org.jetbrains.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec

class NativeInteropPlugin implements Plugin<Project> {

    @Override
    void apply(Project prj) {
        // TODO: handle other source sets
        def srcDir = prj.file("src/main/kotlin")
        def generatedSrcDir = new File(prj.buildDir, "nativeInteropStubs/kotlin")
        def nativeLibsDir = new File(prj.buildDir, "nativelibs")

        prj.configurations {
            interopStubGenerator
        }

        prj.dependencies {
            compile project(path: ':Interop:Runtime')
            interopStubGenerator project(path: ":Interop:StubGenerator")
        }

        prj.task("genInteropStubs", type: JavaExec) {
            classpath = prj.configurations.interopStubGenerator
            main = "org.jetbrains.kotlin.native.interop.gen.jvm.MainKt"
            args = [srcDir, generatedSrcDir, nativeLibsDir]
            systemProperties "java.library.path" : new File(prj.findProject(":Interop:Indexer").buildDir, "nativelibs")
            environment "LIBCLANG_DISABLE_CRASH_RECOVERY": "1"
            environment "DYLD_LIBRARY_PATH": "${prj.llvmInstallPath}/lib"

            inputs.files prj.fileTree(srcDir.path).include('**/*.def')

            outputs.dir generatedSrcDir
            outputs.dir nativeLibsDir
        }

        prj.sourceSets {
            main {
                kotlin {
                    srcDirs generatedSrcDir
                }
            }
        }

        prj.tasks.getByName("compileKotlin") {
            dependsOn "genInteropStubs"
        }

        // FIXME: choose tasks more wisely
        prj.tasks.withType(JavaExec) {
            if (name != "genInteropStubs") {
                systemProperties "java.library.path": nativeLibsDir
            }
        }
    }
}
