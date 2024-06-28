// FIR_IDENTICAL
// ISSUE: KT-58028
// FIR_DUMP
// DIAGNOSTICS: -UNUSED_VARIABLE

open class Base(any: Any) {
    companion object {
        val test = 42L
    }
}

class C1<test> : Base(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>test<!>) {
    companion object {
        val test = 12
        val some: Int = test
    }

    val test = ""
    val some: String = test

    fun f() {
        val test = 1.0
        val some: Double = test
    }
}

class C2<test> : Base(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>test<!>) {
    companion object {
        val test = 12
        val some: Int = test
    }

    val some: Int = test

    fun f() {
        val test = 1.0
        val some: Double = test
    }
}

class C3<test> : Base(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Long")!>test<!>) {
    val some: Long = test

    fun f() {
        val test = 1.0
        val some: Double = test
    }
}

class C4<test> {
    val some = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>test<!>

    fun f() {
        val some = <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION!>test<!>
    }
}
