// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

object A {
    @JvmStatic
    val somePropertyFromObject: Int = 0

    @JvmStatic
    fun someFunctionFromObject(str: String): String = str
}

class B {
    companion object {
        @JvmStatic
        val somePropertyFromCompanionObject: Int = 0

        @JvmStatic
        fun someFunctionFromCompanionObject(str: String): String = str
    }
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    // `name` call must be optimized with `ConstEvaluationLowering`
    if (A::somePropertyFromObject.name != "somePropertyFromObject") return "Fail 1"
    if (A::someFunctionFromObject.name != "someFunctionFromObject") return "Fail 2"
    if (B.Companion::somePropertyFromCompanionObject.name != "somePropertyFromCompanionObject") return "Fail 3"
    if (B.Companion::someFunctionFromCompanionObject.name != "someFunctionFromCompanionObject") return "Fail 4"
    return "OK"
}
