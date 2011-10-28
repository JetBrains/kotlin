import kotlin.modules.ModuleSetBuilder

fun defineModules(builder: ModuleSetBuilder) {
    builder.module("hello") {
        source files "HelloNames.kt"
    }
}
