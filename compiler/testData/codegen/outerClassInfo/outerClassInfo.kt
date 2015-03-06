package foo

class Foo {

  default object {
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

    //anonymous lambda in constructor
  val s = { 11 }()

  fun foo() {
        //anonymous lambda
        { }()
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
