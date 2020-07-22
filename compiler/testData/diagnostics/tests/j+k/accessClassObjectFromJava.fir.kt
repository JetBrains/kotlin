class Foo {
  companion object {
    val bar = 1

    fun test(a: <!OTHER_ERROR!>Foo.`object`<!>) {

    }

  }
}