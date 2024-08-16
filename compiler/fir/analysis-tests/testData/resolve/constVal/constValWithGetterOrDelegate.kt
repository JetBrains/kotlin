import kotlin.reflect.KProperty

const val f = 24

const val l = 3
    <!CONST_VAL_WITH_GETTER!>get<!>

const <!MUST_BE_INITIALIZED!>val k: Int<!>
    <!CONST_VAL_WITH_GETTER!>get<!>

const val t: Int
    <!CONST_VAL_WITH_GETTER!>get() = 24<!>

class Test {
    operator fun getValue(nothing: Nothing?, property: KProperty<*>): Int {
        return 123
    }
}

const val delegated: Int by <!CONST_VAL_WITH_DELEGATE!>Test()<!>

const val e: Boolean
    <!CONST_VAL_WITH_GETTER!>get() = false<!>

const val property: String = "123"
    <!CONST_VAL_WITH_GETTER!>get() = field + " 123 123"<!>
