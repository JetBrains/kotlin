import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

inline fun Project.sourceSets(crossinline body: SourceSetsBuilder.() -> Unit) = SourceSetsBuilder(this).body()

class SourceSetsBuilder(val project: Project) {

    inline operator fun String.invoke(crossinline body: SourceSet.() -> Unit): SourceSet {
        val sourceSetName = this
        return project.sourceSets.maybeCreate(sourceSetName).apply {
            none()
            body()
        }
    }
}

fun SourceSet.none() {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}

val SourceSet.projectDefault: Project.() -> Unit
    get() = {
        when (this@projectDefault.name) {
            "main" -> {
                java.srcDirs("src")
                this@projectDefault.resources.srcDir("resources")
            }
            "test" -> {
                java.srcDirs("test", "tests")
            }
        }
    }

val Project.sourceSets: SourceSetContainer
    get() = javaPluginConvention().sourceSets

val Project.mainSourceSet: SourceSet
    get() = javaPluginConvention().mainSourceSet

val Project.testSourceSet: SourceSet
    get() = javaPluginConvention().testSourceSet

val JavaPluginConvention.mainSourceSet: SourceSet
    get() = sourceSets.getByName("main")

val JavaPluginConvention.testSourceSet: SourceSet
    get() = sourceSets.getByName("test")
