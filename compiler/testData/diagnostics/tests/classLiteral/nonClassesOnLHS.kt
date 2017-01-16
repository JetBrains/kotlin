// !DIAGNOSTICS: -UNUSED_VARIABLE

class A

val <!KCLASS_WITH_NULLABLE_ARGUMENT_IN_SIGNATURE!>a1<!> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A?::class<!>
val <!KCLASS_WITH_NULLABLE_ARGUMENT_IN_SIGNATURE!>a2<!> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A??::class<!>

val <!KCLASS_WITH_NULLABLE_ARGUMENT_IN_SIGNATURE!>l1<!> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>List<String>?::class<!>
val <!KCLASS_WITH_NULLABLE_ARGUMENT_IN_SIGNATURE!>l2<!> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>List?::class<!>

fun <T : Any> foo() {
    val t1 = <!TYPE_PARAMETER_AS_REIFIED!>T::class<!>
    val t2 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>T?::class<!>
}

inline fun <reified T : Any> bar() {
    val t3 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>T?::class<!>
}

val m = Map<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>::class
