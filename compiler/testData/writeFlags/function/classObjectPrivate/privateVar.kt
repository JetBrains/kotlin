class Foo {

  {Foo.test}

  class object {
    private var test = "String"
  }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo$object, setTest
// FLAGS: ACC_PRIVATE, ACC_FINAL