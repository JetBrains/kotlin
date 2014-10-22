// "Change type from 'String' to '(ModuleBuilder) -> Module'" "true"
import kotlin.modules.ModuleBuilder
import kotlin.modules.Module

fun foo(f: ((ModuleBuilder) -> Module) -> String) {
    foo {
        (f: (ModuleBuilder) -> Module) -> "42"
    }
}