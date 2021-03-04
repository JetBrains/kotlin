package toplevelObjectDeclarations
  open class Foo(y : Int) {
    open fun foo() : Int = 1
  }

  <error descr="[NO_VALUE_FOR_PARAMETER] No value passed for parameter 'y'">class T : <error descr="[SUPERTYPE_NOT_INITIALIZED] This type has a constructor, and thus must be initialized here">Foo</error> {}</error>

  object A : <error descr="[SUPERTYPE_NOT_INITIALIZED] This type has a constructor, and thus must be initialized here">Foo</error> {
    val x : Int = 2

    fun test() : Int {
      return x + foo(<error descr="[NO_VALUE_FOR_PARAMETER] No value passed for parameter 'y'">)</error>
    }
  }

  object B : A {}

  val x = A.foo()

  val y = object : Foo(x) {
    init {
      x + 12
    }

    override fun foo() : Int = 1
  }

  val z = y.foo()
