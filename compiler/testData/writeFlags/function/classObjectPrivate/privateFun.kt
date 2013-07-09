class Foo {

  {Foo.test()}

  class object {
    private fun test() {

    }
  }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo$object, test
// FLAGS: ACC_PRIVATE, ACC_FINAL