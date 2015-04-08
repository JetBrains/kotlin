class Foo {

  init {Foo.test}

  companion object {
    private var test = "String"
  }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo$Companion, setTest
// FLAGS: ACC_PRIVATE, ACC_FINAL