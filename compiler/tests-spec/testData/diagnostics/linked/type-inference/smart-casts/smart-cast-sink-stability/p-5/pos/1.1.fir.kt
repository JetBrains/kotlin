// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
class Case1() {

    class E(val plus: Inv? = null, val value: Inv? = null)

    class Inv() {
        operator fun invoke(value: Int) = Case1()
    }

    fun foo(e: E) {

        if (e.value != null) {
            run { e.value(1) }
            /*
             [UNSAFE_CALL] (nok)
             Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Case1.Inv?
            */
            e.value(1)

        }
    }
}
