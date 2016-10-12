package org.jetbrains.kotlin

import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.api.internal.file.AbstractFileCollection
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskDependency
import org.gradle.internal.reflect.Instantiator

class NamedNativeInteropConfig extends AbstractFileCollection implements Named {

    private final Project project
    final String name

    private final SourceSet interopStubs
    private final JavaExec genTask

    private String defFile

    private List<String> compilerOpts = []
    private List<String> linkerOpts = []

    void defFile(String value) {
        defFile = value
        genTask.inputs.file(project.file(defFile))
    }

    void compilerOpts(String... values) {
        compilerOpts.addAll(values)
    }

    void linkerOpts(String... values) {
        linkerOpts.addAll(values)
    }

    void includeDirs(String... values) {
        compilerOpts.addAll(values.collect {"-I$it"})
    }

    private File getNativeLibsDir() {
        return new File(project.buildDir, "nativelibs")
    }

    private File getGeneratedSrcDir() {
        return new File(project.buildDir, "nativeInteropStubs/$name/kotlin")
    }

    NamedNativeInteropConfig(Project project, String name) {
        this.name = name
        this.project = project

        interopStubs = project.sourceSets.create(name + "InteropStubs")
        genTask = project.task(interopStubs.getTaskName("gen", ""), type: JavaExec)

        this.configure()
    }

    private void configure() {
        project.tasks.getByName(interopStubs.getTaskName("compile", "Kotlin")) {
            dependsOn genTask
        }

        interopStubs.kotlin.srcDirs generatedSrcDir

        project.dependencies {
            add interopStubs.getCompileConfigurationName(), project(path: ':Interop:Runtime')
        }

        genTask.configure {
            classpath = project.configurations.interopStubGenerator
            main = "org.jetbrains.kotlin.native.interop.gen.jvm.MainKt"
            jvmArgs '-ea'

            systemProperties "java.library.path" : project.files(
                    new File(project.findProject(":Interop:Indexer").buildDir, "nativelibs"),
                    new File(project.findProject(":Interop:Runtime").buildDir, "nativelibs")
            ).asPath
            systemProperties "llvmInstallPath" : project.llvmInstallPath
            environment "LIBCLANG_DISABLE_CRASH_RECOVERY": "1"
            environment "DYLD_LIBRARY_PATH": "${project.llvmInstallPath}/lib"

            outputs.dir generatedSrcDir
            outputs.dir nativeLibsDir

            // defer as much as possible
            doFirst {
                args = [generatedSrcDir, nativeLibsDir, project.file(defFile)]

                args compilerOpts.collect { "-copt:$it" }
                args linkerOpts.collect { "-lopt:$it" }
            }
        }
    }

    @Override
    String getDisplayName() {
        return "Native interop config $name"
    }

    @Override
    Set<File> getFiles() {
        return interopStubs.output.getFiles() +
                interopStubs.compileClasspath.files // TODO: workaround to add Interop:Runtime
    }

    @Override
    TaskDependency getBuildDependencies() {
        return interopStubs.output.getBuildDependencies()
    }
}

class NativeInteropExtension extends AbstractNamedDomainObjectContainer<NamedNativeInteropConfig> {

    private final Project project

    protected NativeInteropExtension(Project project) {
        super(NamedNativeInteropConfig, project.gradle.services.get(Instantiator))
        this.project = project
    }

    @Override
    protected NamedNativeInteropConfig doCreate(String name) {
        return new NamedNativeInteropConfig(project, name)
    }
}

class NativeInteropPlugin implements Plugin<Project> {

    @Override
    void apply(Project prj) {
        prj.extensions.add("kotlinNativeInterop", new NativeInteropExtension(prj))

        def runtimeNativeLibsDir = new File(prj.findProject(':Interop:Runtime').buildDir, 'nativelibs')

        def nativeLibsDir = new File(prj.buildDir, "nativelibs")

        prj.configurations {
            interopStubGenerator
        }

        prj.dependencies {
            interopStubGenerator project(path: ":Interop:StubGenerator")
        }

        // FIXME: choose tasks more wisely
        prj.tasks.withType(JavaExec) {
            if (!name.endsWith("InteropStubs")) {
                systemProperties "java.library.path": prj.files(
                        nativeLibsDir,
                        runtimeNativeLibsDir
                ).asPath
            }
        }
    }
}
