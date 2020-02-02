// IGNORE_BACKEND: WASM
fun foo() {}

fun box(): String {
  return if (foo() == Unit) "OK" else "Fail"
}
