// !WITH_NEW_INFERENCE
fun test(): Array<Int> {
    <!UNSUPPORTED!>[1, 2]<!>
    <!UNSUPPORTED!>[1, 2]<!>[0]
    <!UNSUPPORTED!>[1, 2]<!>.get(0)

    foo(<!UNSUPPORTED!>[""]<!>)

    val <!UNUSED_VARIABLE!>p<!> = <!UNSUPPORTED!>[1, 2]<!> <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+<!> <!OI;UNSUPPORTED!>[3, 4]<!>

    return <!UNSUPPORTED!>[1, 2]<!>
}

fun foo(<!UNUSED_PARAMETER!>a<!>: Array<String> = <!UNSUPPORTED!>[""]<!>) {}

class A(val a: Array<Int> = <!UNSUPPORTED!>[]<!>)