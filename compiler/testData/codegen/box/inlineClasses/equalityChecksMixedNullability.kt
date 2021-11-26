// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val a: String)

fun isEqualNA(x: A?, y: A) = x == y
fun isEqualAN(x: A, y: A?) = x == y

fun box(): String {
    if (isEqualNA(null, A(""))) return "Fail 1"
    if (isEqualAN(A(""), null)) return "Fail 2"
    if (!isEqualNA(A(""), A(""))) return "Fail 3"
    if (!isEqualAN(A(""), A(""))) return "Fail 4"
    return "OK"
}
