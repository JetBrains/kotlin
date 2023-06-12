// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: type-inference, smart-casts, smart-cast-sink-stability -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 1 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: smart cast for the property available through the operator invoke
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-36876
 */
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
