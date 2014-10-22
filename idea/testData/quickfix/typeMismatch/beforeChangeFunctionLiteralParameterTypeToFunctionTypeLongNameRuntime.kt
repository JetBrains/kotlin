// "Change type from 'String' to '(ModuleBuilder) -> Module'" "true"
fun foo(f: ((kotlin.modules.ModuleBuilder) -> kotlin.modules.Module) -> String) {
    foo {
        (f: String<caret>) -> "42"
    }
}