// "Change type from 'String' to 'Module'" "true"
fun foo(f: (kotlin.modules.Module) -> String) {
    foo {
        (x: String<caret>) -> ""
    }
}