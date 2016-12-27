class A(val a:Int) {
  fun foo(i:Int) = a + i
}

fun fortyTwo() = A(41).foo(1)

fun main(args:Array<String>) {
  if (fortyTwo() != 42) throw Error()
}