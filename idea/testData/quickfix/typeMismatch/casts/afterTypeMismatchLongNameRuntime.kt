// "Cast expression 'module' to 'ModuleBuilder'" "true"
// DISABLE-ERRORS

import kotlin.modules.ModuleBuilder

fun foo(): ModuleBuilder {
    val module: kotlin.modules.Module = ModuleBuilder("", "")
    return module as ModuleBuilder
}