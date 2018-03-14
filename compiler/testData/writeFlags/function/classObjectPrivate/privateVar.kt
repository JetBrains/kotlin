class Foo {

  init {Foo.test}

  companion object {
    private var test = "String"
      // Custom setter is needed, otherwise no need to generate setTest
      set(v) { field = v }
  }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo$Companion, setTest
// FLAGS: ACC_PRIVATE, ACC_FINAL