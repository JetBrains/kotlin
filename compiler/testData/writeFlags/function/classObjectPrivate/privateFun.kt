class Foo {

  {Foo.test()}

  default object {
    private fun test() {

    }
  }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo$Default, test
// FLAGS: ACC_PRIVATE, ACC_FINAL