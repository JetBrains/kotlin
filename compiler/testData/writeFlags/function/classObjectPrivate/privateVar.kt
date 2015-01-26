class Foo {

  {Foo.test}

  class object {
    private var test = "String"
  }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo$Default, setTest
// FLAGS: ACC_PRIVATE, ACC_FINAL