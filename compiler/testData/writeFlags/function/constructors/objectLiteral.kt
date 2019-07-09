// IGNORE_BACKEND: JVM_IR
class Foo {
  fun a() {
    val s = object { }
  }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo$a$s$1, <init>
// FLAGS: