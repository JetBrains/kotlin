package foo;

class Foo {

  class object {
      fun objectFoo() { }
  }

  class InnerClass { }

  object InnerObject { }

  fun foo(f : Foo) {
      class LocalClass {}
      class LocalObject {}
  }

  val objectLiteral = object  {
      fun objectLiteralFoo() { }
  }
}

object PackageInnerObject {
    fun PackageInnerObjectFoo() { }
}

val packageObjectLiteral = object {
      fun objectLiteralFoo() { }
}

fun packageMethod(f : Foo) {
    class PackageLocalClass {}
    class PackageLocalObject {}
}