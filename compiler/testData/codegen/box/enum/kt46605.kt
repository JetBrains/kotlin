// KT-55828
// IGNORE_BACKEND_K2: NATIVE
fun call(f: () -> Unit) {
    f()
}

enum class E(val f: () -> String) {
    A({
          var value = "Fail"
          call {
              value = "OK"
          }
          value
    })
}

fun box(): String {
    return E.A.f()
}
