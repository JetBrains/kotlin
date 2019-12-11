// !DIAGNOSTICS: -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_VARIABLE -DEPRECATION
// !WITH_NEW_INFERENCE

inline fun<reified T> foo(block: () -> T): String = block().toString()

inline fun <reified T: Any> javaClass(): Class<T> = <!OTHER_ERROR!>T<!>::class.<!INAPPLICABLE_CANDIDATE!>java<!>

fun box() {
    val a = arrayOf(null!!)
    val b = Array<Nothing?>(5) { null!! }
    val c = foo() { null!! }
    val d = foo<Any> { null!! }
    val e = foo { "1" as Nothing }
    val e1 = foo { "1" as Nothing? }

    val f = javaClass<Nothing>()
}
