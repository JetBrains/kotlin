// !DIAGNOSTICS: -UNUSED_VARIABLE

class A

val a1 = <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>A?::class<!>
val a2 = <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>A??::class<!>

val l1 = <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>List<String>?::class<!>
val l2 = <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>List?::class<!>

fun <T : Any> foo() {
    val t1 = <!TYPE_PARAMETER_AS_REIFIED!>T::class<!>
    val t2 = <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>T?::class<!>
}

inline fun <reified T : Any> bar() {
    val t3 = <!NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>T?::class<!>
}

val m = Map<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>::class
