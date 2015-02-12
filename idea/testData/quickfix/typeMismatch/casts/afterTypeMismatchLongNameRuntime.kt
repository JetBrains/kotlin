// "Cast expression 'module' to 'ModuleBuilder'" "true"
// DISABLE-ERRORS

import kotlin.modules.ModuleBuilder

fun foo(): kotlin.modules.ModuleBuilder {
    val module: kotlin.modules.Module = kotlin.modules.ModuleBuilder("", "")
    return module as ModuleBuilder
}