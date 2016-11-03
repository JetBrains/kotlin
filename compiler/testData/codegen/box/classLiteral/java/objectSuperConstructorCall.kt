// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import kotlin.test.assertEquals

abstract class S<T>(val klass: Class<T>) {
    val result = klass.simpleName
}

object OK : S<OK>(OK::class.java)

class C {
    companion object Companion : S<Companion>(Companion::class.java)
}

fun box(): String {
    assertEquals("Companion", C.Companion.result)
    return OK.result
}
