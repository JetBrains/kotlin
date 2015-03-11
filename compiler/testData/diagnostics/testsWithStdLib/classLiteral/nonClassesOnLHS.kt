// !DIAGNOSTICS: -UNUSED_VARIABLE

class A

val a1 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A?::class<!>
val a2 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A?<!REDUNDANT_NULLABLE!>?<!>::class<!>

val l1 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>List<String>?::class<!>
val l2 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>List?::class<!>

fun foo<T : Any>() {
    val t1 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>T::class<!>
    val t2 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>T?::class<!>
}
