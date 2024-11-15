// RUN_PIPELINE_TILL: FRONTEND
class A

class C {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias TA = A<!>

    fun test(): TA = TA()
}
