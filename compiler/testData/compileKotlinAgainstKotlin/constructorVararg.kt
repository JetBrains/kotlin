// FILE: A.kt

class A(vararg s: String) {

}

// FILE: B.kt

fun main(args: Array<String>) {
  A()
  A("a")
  A("a", "b")
}
