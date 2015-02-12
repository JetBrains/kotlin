// "Change type from 'String' to '(ModuleBuilder) -> Module'" "true"

import kotlin.modules.ModuleBuilder
import kotlin.modules.Module

fun foo(f: ((kotlin.modules.ModuleBuilder) -> kotlin.modules.Module) -> String) {
    foo {
        (f: (ModuleBuilder) -> Module) -> "42"
    }
}