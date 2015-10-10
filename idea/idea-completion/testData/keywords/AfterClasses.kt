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
// EXIST:  enum
// EXIST:  final
// EXIST:  fun
// EXIST:  internal
// EXIST:  object
// EXIST:  open
// EXIST:  private
// EXIST:  protected
// EXIST:  public
// EXIST:  interface
// EXIST:  val
// EXIST:  var
// EXIST:  operator
// EXIST:  infix
// EXIST:  sealed
// EXIST:  data
// EXIST:  inline
// EXIST:  tailrec
// EXIST:  external
// EXIST:  annotation
// EXIST:  const
/*TODO:*/
// EXIST:  inner
/*TODO:*/
// EXIST:  companion object
// NOTHING_ELSE
