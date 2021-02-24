class Foo {
  companion object {
    val bar = 1

    fun test(a: <!UNRESOLVED_REFERENCE!>Foo.`object`<!>) {

    }

  }
}