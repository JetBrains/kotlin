package codegen.inline.lambdaAsAny

import kotlin.test.*

inline fun foo(x: Any) {
    println(if (x === x) "Ok" else "Fail")
}

@Test fun runTest() {
    foo { 42 }
}