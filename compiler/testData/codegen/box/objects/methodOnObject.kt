// IGNORE_BACKEND: WASM
object A {
  fun result() = "OK"
}

fun box(): String = A.result()
