// FILE: 1.kt

fun box() = foo()

// FILE: 2.kt

private val a = "OK"
fun foo() : String {
  return "${a}"
}
