class Test(val prop: String) {

  companion object {
    public const val prop : String = "CO";
  }

}


// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$1
// FLAGS: ACC_PRIVATE, ACC_FINAL

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_STATIC, ACC_PUBLIC, ACC_FINAL