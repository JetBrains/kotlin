// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
class Case1() {

    class E(val plus: Inv? = null, val value: Inv? = null)

    class Inv() {
        operator fun invoke(value: Int) = Case1()
    }

    fun foo(e: E) {

        if (e.plus != null) {
            run { e <!PROPERTY_AS_OPERATOR!>+<!> 1 }

            /*
             [PROPERTY_AS_OPERATOR]  (ok)
             Properties cannot be used in operator conventions: 'invoke' in 'operatorCall.Case1.Inv'
             [UNSAFE_OPERATOR_CALL]  (nok)
             Operator call corresponds to a dot-qualified call 'e.plus(1)' which is not allowed on a nullable receiver 'e'.
            */
            e <!PROPERTY_AS_OPERATOR!>+<!> 1

            e.plus.invoke(1) //ok
        }

    }
}
