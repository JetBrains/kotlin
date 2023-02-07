// WITH_STDLIB
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
