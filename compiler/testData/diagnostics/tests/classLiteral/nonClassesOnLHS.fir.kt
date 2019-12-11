// !DIAGNOSTICS: -UNUSED_VARIABLE

class A

val a1 = A?::class
val a2 = A??::class

val l1 = List<String>?::class
val l2 = List?::class

fun <T : Any> foo() {
    val t1 = <!OTHER_ERROR!>T<!>::class
    val t2 = <!OTHER_ERROR!>T<!>?::class
}

inline fun <reified T : Any> bar() {
    val t3 = <!OTHER_ERROR!>T<!>?::class
}

val m = Map<String>::class
