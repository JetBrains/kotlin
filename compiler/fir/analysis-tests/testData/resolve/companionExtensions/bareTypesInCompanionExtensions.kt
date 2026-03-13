// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidUselessTypeArgumentsIn25 +CompanionBlocksAndExtensions
class C<T>
object O

typealias TA = C<String>
typealias TA2<T> = C<T>

companion fun C.correct1() {}
companion fun TA.correct2() {}
companion fun TA2.correct3() {}
companion fun Array.correct4() {}

companion fun <!COMPANION_EXTENSION_RECEIVER_WITH_TYPE_ARGUMENTS!>C<String><!>.incorrect1() {}
companion fun <T> <!COMPANION_EXTENSION_RECEIVER_WITH_TYPE_ARGUMENTS!>C<T><!>.incorrect2() {}
companion fun <!COMPANION_EXTENSION_RECEIVER_WITH_TYPE_ARGUMENTS!>TA2<String><!>.incorrect3() {}
companion fun <!COMPANION_EXTENSION_RECEIVER_WITH_TYPE_ARGUMENTS!>Array<String><!>.incorrec4() {}
companion inline fun <reified T> <!COMPANION_EXTENSION_RECEIVER_IS_TYPE_PARAMETER!>T<!>.incorrec5() {}
companion fun <!COMPANION_EXTENSION_RECEIVER_IS_OBJECT!>O<!>.incorrec6() {}

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

    Array.correct4()

    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct1()
    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()
    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()

    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct1()
    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()
    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()

    Array<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct4()
}
/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, nullableType,
typeAliasDeclaration, typeParameter */
