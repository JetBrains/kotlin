// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.full.*
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
