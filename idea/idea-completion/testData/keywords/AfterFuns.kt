class A {
    fun foo() {
        bar()
    }

    fun bar() {
        foo()
    }

    <caret>
}

// EXIST:  abstract
// EXIST:  annotation
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
// EXIST:  constructor
// EXIST:  init
/*why?*/
// EXIST:  companion object
// NOTHING_ELSE: true
