fun foo(a:Int, b:Int) = a - b
fun main(args:Array<String>) {
  if (foo(b = 24, a = 42) != 18)
      throw Error()
}