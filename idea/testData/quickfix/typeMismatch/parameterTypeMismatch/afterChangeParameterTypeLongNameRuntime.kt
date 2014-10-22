// "Change parameter 'x' type of function 'bar.foo' to '(Module) -> Int'" "true"
package bar

import kotlin.modules.Module

fun foo(w: Int = 0, x: (Module) -> Int, y: Int = 0, z: (Int) -> Int = {42}) {
    foo(1, {(a: Module) -> 42}<caret>, 1)
}
