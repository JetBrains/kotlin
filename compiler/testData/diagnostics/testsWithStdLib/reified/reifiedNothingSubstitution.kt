// !DIAGNOSTICS: -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_VARIABLE -DEPRECATION

inline fun<reified T> foo(block: () -> T): String = block().toString()

fun box() {
    val a = <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>arrayOf<!>(null!!)
    val b = <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>Array<!><Nothing?>(5) { null!! }
    val c = <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>foo<!>() { null!! }
    val d = foo<Any> { null!! }
    val e = <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>foo<!> { "1" as Nothing }
    val e1 = <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>foo<!> { "1" as Nothing? }

    val f = <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>javaClass<!><Nothing>()
}
