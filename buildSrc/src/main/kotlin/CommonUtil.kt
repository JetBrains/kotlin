@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.AbstractTask
import org.gradle.jvm.tasks.Jar
import java.io.File

inline fun <reified T : Task> Project.task(noinline configuration: T.() -> Unit) = tasks.creating(T::class, configuration)


fun AbstractTask.dependsOnTaskIfExists(task: String, project: Project?, parentProject: Project?) {
    val thisTask = this
    val p = project ?: this.project
    p.afterEvaluate {
        p.tasks.firstOrNull { it.name == task }?.also {
            if (parentProject != null) {
                parentProject.evaluationDependsOn(p.path)
            }
            thisTask.dependsOn(it)
        }
    }
}

fun AbstractTask.dependsOnTaskIfExistsRec(task: String, project: Project? = null, parentProject: Project? = null) {
    dependsOnTaskIfExists(task, project, parentProject)
    (project ?: this.project).subprojects.forEach {
        dependsOnTaskIfExistsRec(task, it, this.project)
    }
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
