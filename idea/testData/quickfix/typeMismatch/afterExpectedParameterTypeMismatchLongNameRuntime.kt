// "Change type from 'String' to 'Module'" "true"

import kotlin.modules.Module

fun foo(f: (kotlin.modules.Module) -> String) {
    foo {
        (x: Module<caret>) -> ""
    }
}