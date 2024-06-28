// NATIVE error: name contains illegal characters: "$"
// IGNORE_BACKEND: NATIVE
// WITH_STDLIB

import kotlin.reflect.KClass
import kotlin.test.assertEquals

fun check(klass: KClass<*>, expectedName: String) {
    assertEquals(expectedName, klass.simpleName)
}

fun localInMethod() {
    fun localInMethod(unused: Any?) {
        class Local
        check(Local::class, "Local")

        class `Local$With$Dollars`
        check(`Local$With$Dollars`::class, "Local\$With\$Dollars")
    }
    localInMethod(null)

    class Local
    check(Local::class, "Local")

    class `Local$With$Dollars`
    check(`Local$With$Dollars`::class, "Local\$With\$Dollars")
}

class LocalInConstructor {
    init {
        class Local
        check(Local::class, "Local")

        class `Local$With$Dollars`
        check(`Local$With$Dollars`::class, "Local\$With\$Dollars")
    }
}

fun box(): String {
    localInMethod()
    LocalInConstructor()
    return "OK"
}
