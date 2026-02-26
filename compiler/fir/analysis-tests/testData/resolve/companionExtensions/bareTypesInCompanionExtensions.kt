// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidUselessTypeArgumentsIn25
class C<T>

typealias TA = C<String>
typealias TA2<T> = C<T>

<!WRONG_MODIFIER_TARGET!>companion<!> fun C.correct1() {}
<!WRONG_MODIFIER_TARGET!>companion<!> fun TA.correct2() {}
<!WRONG_MODIFIER_TARGET!>companion<!> fun TA2.correct3() {}

<!WRONG_MODIFIER_TARGET!>companion<!> fun C<String>.incorrect1() {}
<!WRONG_MODIFIER_TARGET!>companion<!> fun <T> C<T>.incorrect2() {}
<!WRONG_MODIFIER_TARGET!>companion<!> fun TA2<String>.incorrect3() {}

fun test() {
    C.<!UNRESOLVED_REFERENCE!>correct1<!>()
    C.<!UNRESOLVED_REFERENCE!>correct2<!>()
    C.<!UNRESOLVED_REFERENCE!>correct2<!>()

    TA.<!UNRESOLVED_REFERENCE!>correct1<!>()
    TA.<!UNRESOLVED_REFERENCE!>correct2<!>()
    TA.<!UNRESOLVED_REFERENCE!>correct2<!>()

    TA2.<!UNRESOLVED_REFERENCE!>correct1<!>()
    TA2.<!UNRESOLVED_REFERENCE!>correct2<!>()
    TA2.<!UNRESOLVED_REFERENCE!>correct2<!>()

    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.<!UNRESOLVED_REFERENCE!>correct1<!>()
    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.<!UNRESOLVED_REFERENCE!>correct2<!>()
    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.<!UNRESOLVED_REFERENCE!>correct2<!>()

    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.<!UNRESOLVED_REFERENCE!>correct1<!>()
    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.<!UNRESOLVED_REFERENCE!>correct2<!>()
    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.<!UNRESOLVED_REFERENCE!>correct2<!>()
}
/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, nullableType,
typeAliasDeclaration, typeParameter */
