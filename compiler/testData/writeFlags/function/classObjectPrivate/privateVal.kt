class Foo {

  {Foo.test}

  default object {
    private val test = "String"
  }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo$Default, getTest
// FLAGS: ACC_PRIVATE, ACC_FINAL