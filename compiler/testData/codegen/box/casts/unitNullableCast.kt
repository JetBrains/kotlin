fun foo() {}

@Suppress("CAST_NEVER_SUCCEEDS_ERROR")
fun bar(): Int? = foo() as? Int

fun box(): String {
    return if (bar() == null) "OK" else "fail"
}
