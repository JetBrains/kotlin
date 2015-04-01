class Foo {

  init {Foo.test}

  companion object {
    private val test = "String"
  }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo$Companion, getTest
// FLAGS: ACC_PRIVATE, ACC_FINAL