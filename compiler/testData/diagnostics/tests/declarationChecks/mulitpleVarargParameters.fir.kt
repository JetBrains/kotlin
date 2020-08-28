// !DIAGNOSTICS: -UNUSED_PARAMETER -PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED
fun test(vararg x1: Int, vararg x2: Int) {
    fun test2(vararg x1: Int, vararg x2: Int) {
        class LocalClass(vararg x1: Int, vararg x2: Int) {
        <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor(vararg x1: Int, vararg x2: Int, xx: Int)<!> {}
    }
        fun test3(vararg x1: Int, vararg x2: Int) {}
    }
}

fun Any.test(vararg x1: Int, vararg x2: Int, vararg x3: Int) {}

interface I {
    fun test(vararg x1: Int, vararg x2: Int)
}

abstract class C(vararg x1: Int, vararg x2: Int, b: Boolean) {
    fun test(vararg x1: Int, vararg x2: Int) {}

    abstract fun test2(vararg x1: Int, vararg x2: Int)

    class CC(vararg x1: Int, vararg x2: Int, b: Boolean) {
        <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor(vararg x1: Int, vararg x2: Int)<!> {}
        fun test(vararg x1: Int, vararg x2: Int) {}
    }
}

object O {
    fun test(vararg x1: Int, vararg x2: Int) {}

    class CC(vararg x1: Int, vararg x2: Int, b: Boolean) {
        <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor(vararg x1: Int, vararg x2: Int)<!> {}
        fun test(vararg x1: Int, vararg x2: Int) {}
    }
}
