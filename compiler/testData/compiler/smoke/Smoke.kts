import kotlin.modules.ModuleSetBuilder

fun ModuleSetBuilder.defineModules() {
    module("smoke") {
        source files "Smoke.kt"
        jar name System.getProperty("java.io.tmpdir") + "/smoke.jar"
    }
}
