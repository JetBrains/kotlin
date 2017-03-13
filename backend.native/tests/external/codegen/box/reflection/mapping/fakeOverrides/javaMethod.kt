// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.jvm.*

open class TestBase {
    fun id() = 0L
}

class TestChild : TestBase()

fun box(): String {
    if (TestChild::class.memberFunctions.first { it.name == "id" }.javaMethod == null)
        return "No method for TestChild.id()"

    return "OK"
}
