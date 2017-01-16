// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

fun <T, R> foo(x: T): R = TODO()
fun <T, R> bar(x: T, y: R, f: (T) -> R): Pair<T, R?> = TODO()

inline fun <reified T, reified R> baz(x: T, y: R, f: (T) -> R) {}

data class Pair<A, B>(val a: A, val b: B)

fun <T> test(x: T) {
    bar(1, "", ::foo).checkType { _<Pair<Int, String?>>() }
    bar(null, "", ::foo).checkType { _<Pair<Nothing?, String?>>() }
    bar(1, null, ::foo).checkType { _<Pair<Int, Nothing?>>() }
    bar(null, null, ::foo).checkType { _<Pair<Nothing?, Nothing?>>() }
    bar(1, x, ::foo).checkType { _<Pair<Int, T?>>() }

    val s1: Pair<Int, String?> = bar(1, "", ::foo)
    val (a: Int, b: String?) = bar(1, "", ::foo)

    val s2: Pair<Int?, String?> = bar(null, null, ::foo)

    baz<Int?, String?>(null, null, ::foo)

    baz<Int, String?>(<!NULL_FOR_NONNULL_TYPE!>null<!>, null, ::foo)
    baz<Int?, String>(null, <!NULL_FOR_NONNULL_TYPE!>null<!>, ::foo)
    <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>baz<!>(null, "", ::foo)
    <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>baz<!>(1, null, ::foo)
    <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION, REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>baz<!>(null, null, ::foo)

    val s3: Pair<Int, String?> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>bar(null, null, ::<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>)<!>
    val s4: Pair<Int?, String> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>bar(null, null, ::<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>)<!>

    val s5: Pair<Int, String> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>bar(1, "", ::foo)<!>
    val (a1: Int, b1: String) = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>bar(1, "", ::foo)<!>
}