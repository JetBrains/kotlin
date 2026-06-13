class MyClass {
   operator fun invoke(argument: String) {}
   val itself: MyClass get() = this
   val nullableItself: MyClass? get() = this
}

fun main() {
   val s: MyClass? = MyClass()
   s?.itself("1")
   s!!.itself("2")
   s("3")

   s.nullableItself?.invoke("4")
   s.nullableItself!!("5")
}
