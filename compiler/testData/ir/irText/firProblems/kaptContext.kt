// WITH_RUNTIME
// FULL_JDK

import java.io.File

class KaptOptions(
    val projectBaseDir: File?,
    val compileClasspath: List<File>,
    val javaSourceRoots: List<File>,

    val changedFiles: List<File>,
    val compiledSources: List<File>,
    val incrementalCache: File?,
    val classpathChanges: List<String>,

    val sourcesOutputDir: File,
    val classesOutputDir: File,
    val stubsOutputDir: File,
    val incrementalDataOutputDir: File?,

    val processingClasspath: List<File>,
    val processors: List<String>,

    val processingOptions: Map<String, String>,
    val javacOptions: Map<String, String>
) {
    fun getKotlinGeneratedSourcesDirectory(): File {
        return stubsOutputDir
    }
}

fun warn(s: String) {}

open class KaptContext(val options: KaptOptions, val flag: Boolean) {

    init {
        if (flag) {
            // remove all generated sources and classes
            fun deleteAndCreate(dir: File) {
                if (!dir.deleteRecursively()) warn("Unable to delete $dir.")
                if (!dir.mkdir()) warn("Unable to create $dir.")
            }
            deleteAndCreate(options.sourcesOutputDir)
            deleteAndCreate(options.classesOutputDir)
            options.getKotlinGeneratedSourcesDirectory()?.let {
                deleteAndCreate(it)
            }
        }
    }
}