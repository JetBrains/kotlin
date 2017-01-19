// IGNORE_BACKEND: JS, NATIVE
// JS backend does not support Unit well. See KT-13932

val foo: () -> Unit = {}

fun box(): String {
    foo() as Unit
    return "OK"
}