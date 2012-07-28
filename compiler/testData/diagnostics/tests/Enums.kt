enum class List<out T>(val size : Int) {
  Nil : List<Nothing>(0) {
    val a = 1
  }
  Cons<out T>(val head : T, val tail : List<T>) : List<T>(tail.size + 1)

}

val foo = List.Nil
val foo1 = foo.a
