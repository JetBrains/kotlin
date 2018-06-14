// usages in build scripts are not tracked properly
@file:Suppress("unused")

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.the
import java.io.File
import java.util.concurrent.Callable

inline fun <reified T : Task> Project.task(noinline configuration: T.() -> Unit) = tasks.creating(T::class, configuration)

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

fun Project.generator(fqName: String, sourceSet: SourceSet? = null) = smartJavaExec {
    classpath = (sourceSet ?: testSourceSet).runtimeClasspath
    main = fqName
    workingDir = rootDir
}

fun Project.getBooleanProperty(name: String): Boolean? = this.findProperty(name)?.let {
    val v = it.toString()
    if (v.isBlank()) true
    else v.toBoolean()
}

inline fun CopySourceSpec.from(crossinline filesProvider: () -> Any?): CopySourceSpec = from(Callable { filesProvider() })

fun Project.javaPluginConvention(): JavaPluginConvention = the()
