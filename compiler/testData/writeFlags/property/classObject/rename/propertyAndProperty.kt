class Test {

  public var prop: Int = 0;

  default object {
    public val prop: Int = 0;
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$1
// FLAGS: ACC_PRIVATE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_STATIC, ACC_PUBLIC, ACC_FINAL