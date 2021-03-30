  object A {
    val x : Int = 0
  }

  open class Foo {
    fun foo() : Int = 1
  }

  fun test() {
    A.x
    val b = object : Foo() {
    }
    b.foo()

    <error descr="[LOCAL_OBJECT_NOT_ALLOWED] Named object 'B' is a singleton and cannot be local. Try to use anonymous object instead">object B</error> {
      fun foo() {}
    }
    B.foo()
  }

  val bb = <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: B">B</error>.foo()
