class Foo {

  {Foo.test}

  class object {
    private val test = "String"
  }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo$object, getTest
// FLAGS: ACC_PRIVATE, ACC_FINAL