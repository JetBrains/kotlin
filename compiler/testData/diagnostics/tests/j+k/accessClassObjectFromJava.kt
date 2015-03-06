class Foo {
  default object {
    val bar = 1

    fun test(<!UNUSED_PARAMETER!>a<!>: Foo.<!UNRESOLVED_REFERENCE!>`object`<!>) {

    }

  }
}