// IS_APPLICABLE: false
class Foo() {
  fun bar() {
    val o = object {
        val s = this@<caret>Foo
    }
  }
}
