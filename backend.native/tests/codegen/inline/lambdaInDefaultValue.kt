package codegen.inline.lambdaInDefaultValue

import kotlin.test.*

inline fun inlineFun(param: String, lambda: (String) -> String = { it }): String {
    return lambda(param)
}

fun box(): String {
    return inlineFun("OK")
}

@Test fun runTest() {
    println(box())
}