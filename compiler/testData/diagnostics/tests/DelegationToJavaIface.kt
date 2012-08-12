trait One {
  public open fun foo() : Int
  private fun boo() = 10
}
trait Two {
  public open fun foo() : Int
}

trait OneImpl : One {
  public override fun foo() = 1
}
trait TwoImpl : Two {
  public override fun foo() = 2
}

class <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>Test1<!>() : TwoImpl, OneImpl {}
//class Test2(a : One) : One by a, Two {}
class <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>Test3<!>(a : One, b : Two) : Two by b, One by a {}

class TestIface(r : Runnable) : Runnable by r {}
class TestIfaceFakeDelegate(r : Runnable) : Runnable by r {
  public override fun run() {}
}

class TestIfaceExt(list : java.util.List<Runnable>) : java.util.List<Runnable> by list {
  public override fun size() : Int = 0
}

class TestObject(o : Object) : <!DELEGATION_NOT_TO_TRAIT!>Object<!> by o {}