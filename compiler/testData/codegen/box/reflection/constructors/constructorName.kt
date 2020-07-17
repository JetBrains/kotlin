// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

class A

fun box(): String {
    assertEquals("<init>", ::A.name)
    return "OK"
}
