// !DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_VARIABLE -REIFIED_TYPE_PARAMETER_NO_INLINE

inline fun<reified T> foo(block: () -> T): String = block().toString()

fun box() {
    val a = <!REIFIED_TYPE_NOTHING_SUBSTITUTION!>array<!>(null!!)
    val b = <!REIFIED_TYPE_NOTHING_SUBSTITUTION!>Array<!><Nothing?>(5) { null!! }
    val c = <!REIFIED_TYPE_NOTHING_SUBSTITUTION!>foo<!>() { null!! }
    val d = foo<Any> { null!! }
    val e = <!REIFIED_TYPE_NOTHING_SUBSTITUTION!>foo<!> { "1" as Nothing }
    val e1 = <!REIFIED_TYPE_NOTHING_SUBSTITUTION!>foo<!> { "1" as Nothing? }

    val f = <!REIFIED_TYPE_NOTHING_SUBSTITUTION!>javaClass<!><Nothing>()
}
