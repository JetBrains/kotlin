private class A {
  public inner class Foo(val a: Int = 1) {}
}

// CLASS: A$Foo
// HAS_DEFAULT_CONSTRUCTOR: false
