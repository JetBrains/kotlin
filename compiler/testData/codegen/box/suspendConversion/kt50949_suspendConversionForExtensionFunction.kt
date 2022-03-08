// !LANGUAGE: +SuspendConversion
// IGNORE_BACKEND: JVM

fun box(): String {
    val foo: String.(suspend () -> Unit) -> String = { this }
    val f: () -> Unit = {}
    return "OK".foo(f)
}
