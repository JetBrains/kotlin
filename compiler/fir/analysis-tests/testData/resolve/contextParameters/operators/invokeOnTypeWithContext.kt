// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
class A

class Test1: context(A)()->Unit {
    override fun invoke(p1: A) { }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Test2<!>: context(A)()->Unit {
    context(a: A)
    <!NOTHING_TO_OVERRIDE!>override<!> fun invoke() { }
}

fun usage() {
    Test1()(A())

    with(A()) {
        Test1()()
    }
}