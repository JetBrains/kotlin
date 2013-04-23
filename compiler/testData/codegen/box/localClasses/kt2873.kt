fun foo() : String {
    val u = {
        class B(val data : String)
        B("OK").data
    }
    return u()
}

fun main(args: Array<String>) {
    foo()
}

fun box(): String {
  return foo()
}