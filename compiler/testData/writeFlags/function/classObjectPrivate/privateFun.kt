class Foo {

  init {Foo.test()}

  companion object {
    private fun test() {

    }
  }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo$Companion, test
// FLAGS: ACC_PRIVATE, ACC_FINAL