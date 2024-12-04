// MODULE: m1-common

expect class NonConstNonConst {
    companion object {
        val prop: Int
    }
}

expect class NonConstConst {
    companion object {
        val prop: Int
    }
}

expect class ConstNonConst {
    companion object {
        <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val prop: Int
    }
}

expect class ConstConst {
    companion object {
        <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val prop: Int
    }
}

expect val NonConstNonConstTl: Int
expect val NonConstConstTl: Int
expect <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val ConstNonConstTl: Int
expect <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val ConstConstTl: Int

// MODULE: m2-jvm()()(m1-common)

class NonConstImpl {
    companion object {
        val prop: Int get() = 42
    }
}

class ConstImpl {
    companion object {
        const val prop: Int = 42
    }
}

// actuals

actual typealias NonConstNonConst = NonConstImpl
actual typealias NonConstConst = ConstImpl
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>ConstNonConst<!> = NonConstImpl
actual typealias ConstConst = ConstImpl

actual val NonConstNonConstTl: Int get() = 42
actual const val NonConstConstTl: Int = 42
<!ACTUAL_WITHOUT_EXPECT!>actual<!> val ConstNonConstTl: Int get() = 42
actual const val ConstConstTl: Int = 42
