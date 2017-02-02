fun foo(vararg x: Any?) {}
fun bar() = foo()

fun main(arg:Array<String>) {
  bar()
}