package toplevelObjectDeclarations
  open class Foo(y : Int) {
    open fun foo() : Int = 1
  }

  <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): toplevelObjectDeclarations/Foo.Foo">class T : <error descr="[SUPERTYPE_NOT_INITIALIZED] This type has a constructor, and thus must be initialized here">Foo</error> {}</error>

  <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): toplevelObjectDeclarations/Foo.Foo">object A : <error descr="[SUPERTYPE_NOT_INITIALIZED] This type has a constructor, and thus must be initialized here">Foo</error> {
    val x : Int = 2

    fun test() : Int {
      return x + foo()
    }
  }</error>

  object B : A {}

  val x = A.foo()

  val y = object : Foo(x) {
    init {
      x + 12
    }

    override fun foo() : Int = 1
  }

  val z = y.foo()
