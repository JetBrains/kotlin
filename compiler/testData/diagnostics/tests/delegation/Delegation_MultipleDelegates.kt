// !DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS
interface One {
  public <!REDUNDANT_OPEN_IN_INTERFACE!>open<!> fun foo() : Int
  private fun boo() = 10
}
interface Two {
  public <!REDUNDANT_OPEN_IN_INTERFACE!>open<!> fun foo() : Int
}

interface OneImpl : One {
  public override fun foo() = 1
}
interface TwoImpl : Two {
  public override fun foo() = 2
}

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class Test1<!>() : TwoImpl, OneImpl {}
class Test2(a : One) : One by a, Two {}
<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Test3<!>(a : One, b : Two) : Two by b, One by a {}