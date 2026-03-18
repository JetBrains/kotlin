// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidUselessTypeArgumentsIn25 +CompanionBlocksAndExtensions
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
    C.correct1()
    C.correct2()
    C.correct2()

    TA.correct1()
    TA.correct2()
    TA.correct2()

    TA2.correct1()
    TA2.correct2()
    TA2.correct2()

    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct1()
    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()
    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()

    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct1()
    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()
    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()
}
/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, nullableType,
typeAliasDeclaration, typeParameter */
