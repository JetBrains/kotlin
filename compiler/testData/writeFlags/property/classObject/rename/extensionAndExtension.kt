class A { }
class B { }

class Test {

  class object {
    public val A.prop: Int = 0;
    public val B.prop: Int = 0;
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$ext
// FLAGS: ACC_PRIVATE, ACC_FINAL, ACC_STATIC

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$ext$1
// FLAGS: ACC_PRIVATE, ACC_FINAL, ACC_STATIC
