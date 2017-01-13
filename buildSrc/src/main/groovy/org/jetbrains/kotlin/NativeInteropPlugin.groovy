package org.jetbrains.kotlin

import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.api.internal.file.AbstractFileCollection
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskDependency
import org.gradle.internal.reflect.Instantiator

class NamedNativeInteropConfig implements Named {

    private final Project project
    final String name

    private final SourceSet interopStubs
    private final JavaExec genTask

    private String defFile

    private String pkg;

    private List<String> compilerOpts = []
    private FileCollection headers;
    private String linker
    private List<String> linkerOpts = []
    private FileCollection linkFiles;
    private List<String> linkTasks = []

    Configuration configuration

    void defFile(String value) {
        defFile = value
        genTask.inputs.file(project.file(defFile))
    }

    void pkg(String value) {
        pkg = value
    }

    void compilerOpts(String... values) {
        compilerOpts.addAll(values)
    }

    void headers(FileCollection files) {
        dependsOnFiles(files)
        headers = headers + files
    }

    void linker(String value) {
        linker = value
    }

    void linkerOpts(String... values) {
        this.linkerOpts(values.toList())
    }

    void linkerOpts(List<String> values) {
        linkerOpts.addAll(values)
    }

    void dependsOn(Object... deps) {
        // TODO: add all files to inputs
        genTask.dependsOn(deps)
    }

    private void dependsOnFiles(FileCollection files) {
        dependsOn(files)
        genTask.inputs.files(files)
    }

    void link(FileCollection files) {
        linkFiles = linkFiles + files
        dependsOnFiles(files)
    }

    void linkOutputs(Task task) {
        linkOutputs(task.name)
    }

    void linkOutputs(String task) {
        linkTasks += task
        dependsOn(task)

        final Project prj;
        final String taskName;
        int index = task.lastIndexOf(':')
        if (index != -1) {
            prj = project.project(task.substring(0, index))
            taskName = task.substring(index + 1)
        } else {
            prj = project
            taskName = task
        }

        prj.tasks.matching { it.name == taskName }.all { // TODO: it is a hack
            this.dependsOnFiles(it.outputs.files)
        }
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

        this.headers = project.files()
        this.linkFiles = project.files()

        interopStubs = project.sourceSets.create(name + "InteropStubs")
        genTask = project.task(interopStubs.getTaskName("gen", ""), type: JavaExec)
        configuration = project.configurations.create(interopStubs.name)

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
            systemProperties "llvmInstallPath" : project.llvmDir
            environment "LIBCLANG_DISABLE_CRASH_RECOVERY": "1"
            environment "DYLD_LIBRARY_PATH": "${project.llvmDir}/lib"
            environment "LD_LIBRARY_PATH": "${project.llvmDir}/lib"

            outputs.dir generatedSrcDir
            outputs.dir nativeLibsDir

            // defer as much as possible
            doFirst {
                linkTasks.each {
                    linkerOpts += project.tasks.getByPath(it).outputs.files.files
                }

                linkerOpts += linkFiles.files

                args = [generatedSrcDir, nativeLibsDir]
                if (defFile != null) {
                    args "-def:" + project.file(defFile)
                }

                if (pkg != null) {
                    args "-pkg:" + pkg
                }

                if (linker != null) {
                    args "-linker:" + linker
                }

                // TODO: the interop plugin should probably be reworked to execute clang from build scripts directly
                environment['PATH'] = project.files(project.clangPath).asPath +
                        File.pathSeparator + environment['PATH']

                compilerOpts += project.hostClangArgs
                linkerOpts   += project.hostClangArgs

                args compilerOpts.collect { "-copt:$it" }
                args linkerOpts.collect { "-lopt:$it" }

                headers.files.each {
                    args "-h:$it"
                }

                if (project.hasProperty("shims")) {
                    args  "-shims:$project.ext.shims"
                }

            }
        }

        this.configuration.extendsFrom project.configurations[interopStubs.runtimeConfigurationName]
        project.dependencies.add(this.configuration.name, interopStubs.output)
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
