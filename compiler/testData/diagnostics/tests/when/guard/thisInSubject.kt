// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN

open class Super {
    fun ThisInSubject() {
        return when (this) {
            is Sub1 <!UNSUPPORTED_FEATURE!>if this.prop1.length > 0<!> -> Unit
            is Sub2 <!UNSUPPORTED_FEATURE!>if this.prop2 > 0<!> -> Unit
            else -> Unit
        }
    }
}

class Sub1(val prop1: String) : Super()
class Sub2(val prop2: Int) : Super()
