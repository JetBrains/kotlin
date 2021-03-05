import kotlin.reflect.KProperty

const val f = 24

const val l = 3
    <!CONST_VAL_WITH_GETTER!>get<!>

<!MUST_BE_INITIALIZED!><!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val k: Int<!>
    <!CONST_VAL_WITH_GETTER!>get<!>

<!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val t: Int
    <!CONST_VAL_WITH_GETTER!>get() = 24<!>

class Test {
    operator fun getValue(nothing: Nothing?, property: KProperty<*>): Int {
        return 123
    }
}

<!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val delegated: Int by <!CONST_VAL_WITH_DELEGATE!>Test()<!>

<!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val e: Boolean
    <!CONST_VAL_WITH_GETTER!>get() = false<!>

const val property: String = "123"
    <!CONST_VAL_WITH_GETTER!>get() = field + " 123 123"<!>
