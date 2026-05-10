// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidUselessTypeArgumentsIn25 +CompanionBlocksAndExtensions
class C<T>
object O

typealias TA = C<String>
typealias TA2<T> = C<T>
typealias TA3<T> = C<T>?

companion fun C.correct1() {}
companion fun TA.correct2() {}
companion fun TA2.correct3() {}
companion fun TA3.correct4() {}
companion fun Array.correct5() {}

companion fun <!COMPANION_EXTENSION_RECEIVER_WITH_TYPE_ARGUMENTS!>C<String><!>.incorrect1() {}
companion fun <T> <!COMPANION_EXTENSION_RECEIVER_WITH_TYPE_ARGUMENTS!>C<T><!>.incorrect2() {}
companion fun <!COMPANION_EXTENSION_RECEIVER_WITH_TYPE_ARGUMENTS!>TA2<String><!>.incorrect3() {}
companion fun <!COMPANION_EXTENSION_RECEIVER_WITH_TYPE_ARGUMENTS!>Array<String><!>.incorrect4() {}
companion inline fun <reified T> <!COMPANION_EXTENSION_RECEIVER_IS_TYPE_PARAMETER!>T<!>.incorrect5() {}
companion inline fun <reified T> <!COMPANION_EXTENSION_RECEIVER_IS_TYPE_PARAMETER!>(T & Any)<!>.incorrect5_1() {}
companion fun <!COMPANION_EXTENSION_RECEIVER_IS_OBJECT!>O<!>.incorrect6() {}
companion fun <!COMPANION_EXTENSION_NULLABLE_RECEIVER!>C?<!>.incorrect7() {}
companion fun <!DYNAMIC_RECEIVER_NOT_ALLOWED, UNSUPPORTED!>dynamic<!>.incorrect8() {}

fun test() {
    C.correct1()
    C.correct2()
    C.correct2()

    TA.correct1()
    TA.correct2()
    TA.correct3()
    TA.correct4()

    TA2.correct1()
    TA2.correct2()
    TA2.correct3()
    TA2.correct4()

    TA3.correct1()
    TA3.correct2()
    TA3.correct3()
    TA3.correct4()

    Array.correct5()

    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct1()
    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()
    C<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()

    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct1()
    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()
    TA2<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.correct2()

    Array<!TYPE_ARGUMENTS_NOT_ALLOWED!><String><!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>correct4<!>()
}
/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, nullableType,
typeAliasDeclaration, typeParameter */
