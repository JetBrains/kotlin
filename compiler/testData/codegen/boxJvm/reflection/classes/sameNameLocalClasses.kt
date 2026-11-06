// ISSUE: KT-84623
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.full.declaredFunctions
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

fun instance(flag: Boolean, value: String): Any = if (flag) {
    class Local {
        fun get() = "O" + value
    }
    Local()
} else {
    class Local {
        fun get() = value + "K"
    }
    Local()
}

fun box(): String {
    val first = instance(true, "K")
    val second = instance(false, "O")
    assertNotEquals(first.javaClass, second.javaClass)
    for (value in listOf(first, second)) {
        assertEquals("Local", value::class.simpleName)
        assertEquals(null, value::class.qualifiedName)
        assertEquals("OK", value::class.declaredFunctions.single { it.name == "get" }.call(value))
    }
    return "OK"
}
