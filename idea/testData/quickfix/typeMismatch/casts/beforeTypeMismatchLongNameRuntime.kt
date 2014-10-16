// "Cast expression 'module' to 'ModuleBuilder'" "true"
// DISABLE-ERRORS

fun foo(): kotlin.modules.ModuleBuilder {
    val module: kotlin.modules.Module = kotlin.modules.ModuleBuilder("", "")
    return module<caret>
}