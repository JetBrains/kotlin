@file:Suppress("unused") // usages in build scripts are not tracked properly

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.AbstractTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import java.io.File

inline fun <reified T : Task> Project.task(noinline configuration: T.() -> Unit) = tasks.creating(T::class, configuration)

fun Project.callGroovy(name: String, vararg args: Any?): Any? {
    return (property(name) as Closure<*>).call(*args)
}

inline fun<T: Any> Project.withJavaPlugin(crossinline body: () -> T?): T? {
    var res: T? = null
    pluginManager.withPlugin("java") {
        res = body()
    }
    return res
}

fun Project.getCompiledClasses(): SourceSetOutput? = withJavaPlugin {
    the<JavaPluginConvention>().sourceSets.getByName("main").output
}

fun Project.getSources(): SourceDirectorySet? = withJavaPlugin {
    the<JavaPluginConvention>().sourceSets.getByName("main").allSource
}

fun Project.getResourceFiles(): SourceDirectorySet? = withJavaPlugin {
    the<JavaPluginConvention>().sourceSets.getByName("main").resources
}

fun File(root: File, vararg children: String): File = children.fold(root, { f, c -> File(f, c) })
fun File(root: String, vararg children: String): File = children.fold(File(root), { f, c -> File(f, c) })

var Project.jvmTarget: String?
    get() = extra.takeIf { it.has("jvmTarget") }?.get("jvmTarget") as? String
    set(v) { extra["jvmTarget"] = v }

var Project.javaHome: String?
    get() = extra.takeIf { it.has("javaHome") }?.get("javaHome") as? String
    set(v) { extra["javaHome"] = v }

fun Project.generator(fqName: String) = task<JavaExec> {
    classpath = the<JavaPluginConvention>().sourceSets["test"].runtimeClasspath
    main = fqName
    workingDir = rootDir
}
