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
// EXIST:  in
/*why?*/
// EXIST:  inner
// EXIST:  internal
// EXIST:  object
// EXIST:  open
// EXIST:  out
/*why?*/
// EXIST:  reified
/*why?*/
// EXIST:  override
// EXIST:  private
// EXIST:  protected
// EXIST:  public
// EXIST:  interface
// EXIST:  val
// EXIST:  var
// EXIST:  vararg
/*why?*/
// EXIST:  companion object
// EXIST:  sealed
// EXIST:  lateinit
// EXIST:  data
// EXIST:  inline
// EXIST:  noinline
// EXIST:  tailrec
// EXIST:  external
// EXIST:  annotation
/*TODO*/
// NOTHING_ELSE
