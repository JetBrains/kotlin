// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NullableNothingInReifiedPosition
// DIAGNOSTICS: -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_VARIABLE -DEPRECATION

inline fun<reified T> foo(block: () -> T): String = block().toString()

inline fun <reified T: Any> javaClass(): Class<T> = T::class.java

fun box() {
    val a = <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION, UNSUPPORTED!>arrayOf<!>(null!!)
    val b = <!UNSUPPORTED("'Array<Nothing?>' is not supported on the JVM.")!>Array<!><Nothing?>(5) { null!! }
    val c = <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>foo<!>() { null!! }
    val d = foo<Any> { null!! }
    val e = <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>foo<!> { "1" <!CAST_NEVER_SUCCEEDS!>as<!> Nothing }
    val e1 = foo { "1" <!CAST_NEVER_SUCCEEDS!>as<!> Nothing? }

    val f = javaClass<<!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>Nothing<!>>()
}
