class Test {

  public val prop: Int = 0;

  class object {
    public var Test.prop: Int = 0;
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_PRIVATE, ACC_FINAL

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$ext
// FLAGS: ACC_STATIC, ACC_PRIVATE
