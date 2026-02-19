// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// RENDER_DIAGNOSTICS_FULL_TEXT

package foo

enum class MyEnum {
    A, B
}

class Test {
    companion object {
        val A = Any()
    }

    fun test1wrong(x: MyEnum): Boolean {
        return x == A
    }

    fun test1enum(x: MyEnum): Boolean {
        return x == MyEnum.A
    }

    fun test1companion(x: MyEnum): Boolean {
        return x == Companion.A
    }

    fun test2wrong(x: MyEnum): Int = when (x) {
        A -> 1
        B -> 2
    }

    fun test2enum(x: MyEnum): Int = when (x) {
        MyEnum.A -> 1
        B -> 2
    }

    fun test2companion(x: MyEnum): Int = when (x) {
        Companion.A -> 1
        B -> 2
    }
}
