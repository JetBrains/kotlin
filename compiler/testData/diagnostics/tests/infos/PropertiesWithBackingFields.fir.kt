abstract class Test() {
    abstract val x : Int
    abstract val x1 : Int get
    abstract val x2 : Int <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = 1<!>

    val a : Int
    val b : Int get
    val c = 1

    val c1 = 1
      get
    val c2 : Int
        get() = 1
    val c3 : Int
        get() { return 1 }
    val c4 : Int
        get() = 1
    val c5 : Int
        get() = field + 1

    abstract var y : Int
    abstract var y1 : Int get
    abstract var y2 : Int set
    abstract var y3 : Int set get
    abstract var y4 : Int set <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = 1<!>
    abstract var y5 : Int <!ABSTRACT_PROPERTY_WITH_SETTER!>set(x) {}<!> <!ABSTRACT_PROPERTY_WITH_GETTER!>get() = 1<!>
    abstract var y6 : Int <!ABSTRACT_PROPERTY_WITH_SETTER!>set(x) {}<!>

    var v : Int
    var v1 : Int get
    var v2 : Int get set
    var v3 : Int get() = 1; set
    var v4 : Int get() = 1; set(x){}

    var v5 : Int get() = 1; set(x){field = x}
    var v6 : Int get() = field + 1; set(x){}

  abstract val v7 : Int get
  abstract var v8 : Int get set
  var v9 : Int set
  var v10 : Int  get
  abstract val v11 : Int abstract get
  abstract var v12 : Int abstract get abstract set

}

open class Super(i : Int)

class TestPCParameters(w : Int, x : Int, val y : Int, var z : Int) : Super(w) {

  val xx = w

  init {
    w + 1
  }

  fun foo() = <!UNRESOLVED_REFERENCE!>x<!>

}