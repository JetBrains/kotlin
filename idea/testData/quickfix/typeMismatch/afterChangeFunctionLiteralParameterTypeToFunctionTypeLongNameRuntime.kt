// "Change type from 'String' to '(ModuleBuilder) -> Module'" "true"

import kotlin.modules.Module
import kotlin.modules.ModuleBuilder

fun foo(f: ((kotlin.modules.ModuleBuilder) -> kotlin.modules.Module) -> String) {
    foo {
        (f: (ModuleBuilder) -> Module) -> "42"
    }
}