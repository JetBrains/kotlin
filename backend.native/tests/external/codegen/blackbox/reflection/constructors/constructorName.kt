// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.test.assertEquals

class A

fun box(): String {
    assertEquals("<init>", ::A.name)
    return "OK"
}
