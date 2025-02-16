// WITH_STDLIB
// LINK_VIA_SIGNATURES_K1
// DUMP_SIGNATURES

// MODULE: maven
// FILE: MavenProject.kt
package maven

interface MavenProject

// MODULE: lib(maven)
// FILE: lib.kt
import maven.MavenProject

abstract class AbstractMavenImportHandler {
    abstract fun getOptions(enabledCompilerPlugins: List<String>, compilerPluginOptions: List<String>): List<String>?

    protected open fun getOptions(
        mavenProject: MavenProject,
        enabledCompilerPlugins: List<String>,
        compilerPluginOptions: List<String>
    ): List<String>? = getOptions(enabledCompilerPlugins, compilerPluginOptions)
}

// MODULE: main(lib)
// FILE: sam.kt

class SamWithReceiverMavenProjectImportHandler : AbstractMavenImportHandler() {
    override fun getOptions(enabledCompilerPlugins: List<String>, compilerPluginOptions: List<String>): List<String>? {
        return null
    }
}

// FILE: main.kt

fun box(): String {
    val result = SamWithReceiverMavenProjectImportHandler()
    return result.getOptions(emptyList(), emptyList())?.get(0) ?: "OK"
}
