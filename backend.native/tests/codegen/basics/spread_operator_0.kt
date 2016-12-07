fun main(arg:Array<String>) {
  val list0 = _arrayOf("K", "o", "t", "l", "i", "n")
  val list1 = _arrayOf("l", "a","n", "g", "u", "a", "g", "e")
  val list = foo(list0, list1)
  println(list.toString())
}


fun foo(a:Array<out String>, b:Array<out String>) = listOf(*a," ", "i", "s", " ", "c", "o", "o", "l", " ", *b)


fun _arrayOf(vararg arg:String) = arg