// ISSUE: KT-75316
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    OK
}

sealed class MySealed {
    data object Ok : MySealed()
}

class MyClass {
    companion object {
        val INSTANCE = MyClass()
    }

    override fun toString() = "OK"
}

fun <T> myRun(b: () -> T): T = b()

fun box(): String {
    val t1: MyEnum = OK
    if (t1.name != "OK") return "fail 1"

    val t2: MySealed = Ok
    if (t2.toString() != "Ok") return "fail 2"

    val t3: MyClass = INSTANCE
    if (t3.toString() != "OK") return "fail 3"

    val t4: MyEnum = myRun<MyEnum> {
        OK
    }
    if (t4.name != "OK") return "fail 4"

    val t5: MySealed = myRun {
        Ok
    }
    if (t5.toString() != "Ok") return "fail 5"

    val t6: MyClass = myRun {
        INSTANCE
    }
    if (t6.toString() != "OK") return "fail 6"

    val b = "".hashCode() == 0

    val t7: MyEnum = when {
        b -> OK
        else -> null!!
    }
    if (t7.name != "OK") return "fail 7"

    val t8: MySealed = when {
        b -> Ok
        else -> null!!
    }
    if (t8.toString() != "Ok") return "fail 8"

    val t9: MyClass = when {
        b -> INSTANCE
        else -> null!!
    }
    if (t9.toString() != "OK") return "fail 9"

    return "OK"
}
