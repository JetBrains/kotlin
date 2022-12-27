// usages in build scripts are not tracked properly
@file:Suppress("unused")

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.*
import proguard.gradle.ProGuardTask
import java.io.File
import java.util.concurrent.Callable

inline fun <reified T : Task> Project.task(noinline configuration: T.() -> Unit) = tasks.registering(T::class, configuration)

inline fun <reified T : Task> Project.eagerTask(noinline configuration: T.() -> Unit) = tasks.creating(T::class, configuration)

fun Project.callGroovy(name: String, vararg args: Any?): Any? {
    return (property(name) as Closure<*>).call(*args)
}

inline fun <T : Any> Project.withJavaPlugin(crossinline body: () -> T?): T? {
    var res: T? = null
    pluginManager.withPlugin("java") {
        res = body()
    }
    return res
}

fun Project.getCompiledClasses(): SourceSetOutput? = withJavaPlugin { mainSourceSet.output }

fun Project.getSources(): SourceDirectorySet? = withJavaPlugin { mainSourceSet.allSource }

fun Project.getResourceFiles(): SourceDirectorySet? = withJavaPlugin { mainSourceSet.resources }

fun fileFrom(root: File, vararg children: String): File = children.fold(root) { f, c -> File(f, c) }

fun fileFrom(root: String, vararg children: String): File = children.fold(File(root)) { f, c -> File(f, c) }

var Project.jvmTarget: String?
    get() = extra.takeIf { it.has("jvmTarget") }?.get("jvmTarget") as? String
    set(v) {
        extra["jvmTarget"] = v
    }

var Project.javaHome: String?
    get() = extra.takeIf { it.has("javaHome") }?.get("javaHome") as? String
    set(v) {
        extra["javaHome"] = v
    }

fun Project.generator(fqName: String, sourceSet: SourceSet? = null, configure: JavaExec.() -> Unit = {}) = smartJavaExec {
    group = "Generate"
    classpath = (sourceSet ?: testSourceSet).runtimeClasspath
    mainClass.set(fqName)
    workingDir = rootDir
    systemProperty("line.separator", "\n")
    systemProperty("idea.ignore.disabled.plugins", "true")
    configure()
}

fun Project.getBooleanProperty(name: String): Boolean? = this.findProperty(name)?.let {
    val v = it.toString()
    if (v.isBlank()) true
    else v.toBoolean()
}

inline fun CopySourceSpec.from(crossinline filesProvider: () -> Any?): CopySourceSpec = from(Callable { filesProvider() })

fun Project.javaPluginExtension(): JavaPluginExtension = extensions.getByType()

fun Project.findJavaPluginExtension(): JavaPluginExtension? = extensions.findByType()

fun JavaExec.pathRelativeToWorkingDir(file: File): String = file.relativeTo(workingDir).invariantSeparatorsPath

fun Task.singleOutputFile(): File = when (this) {
    is AbstractArchiveTask -> archiveFile.get().asFile
    is ProGuardTask -> project.file(outJarFiles.single()!!)
    else -> outputs.files.singleFile
}

val Project.isConfigurationCacheDisabled
    get() = (gradle.startParameter as? org.gradle.api.internal.StartParameterInternal)?.configurationCache?.get() != true

val Project.isIdeaActive
    get() = providers.systemProperty("idea.active").isPresent

val Project.intellijCommunityDir: File
    get() = rootDir.resolve("intellij/community").takeIf { it.isDirectory } ?: rootDir.resolve("intellij")