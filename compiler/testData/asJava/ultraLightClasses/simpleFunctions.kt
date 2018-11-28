class Foo {
  open fun bar(a: Int, b:Any, c:Foo): Unit {}
  internal fun bar2(a: Sequence, b: Unresolved) {}
  private fun bar3(x: Foo.Inner, vararg y: Inner) = "str"
  fun bar4() = 42

  public fun nullableVararg(vararg o: Any?): Unit

  operator fun plus(increment: Int): Foo {}
  fun String.onString(a: (Int) -> Any?): Foo {}

  class Inner {}
}
