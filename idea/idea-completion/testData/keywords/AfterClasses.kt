class A {
    fun foo() {
        bar()
    }
}

class B {
    fun bar() {
        foo()
    }
}

<caret>

// EXIST:  abstract
// EXIST:  class
// EXIST:  enum class
// EXIST:  final
// EXIST:  fun
// EXIST:  internal
// EXIST:  object
// EXIST:  open
// EXIST:  private
// EXIST:  public
// EXIST:  interface
// EXIST:  val
// EXIST:  var
// EXIST:  operator
// EXIST:  infix
// EXIST:  sealed class
// EXIST:  data
// EXIST:  inline
// EXIST:  tailrec
// EXIST:  external
// EXIST:  annotation class
// EXIST:  const
// EXIST:  suspend
// EXIST:  typealias
// EXIST:  expect
// EXIST:  actual
// NOTHING_ELSE
