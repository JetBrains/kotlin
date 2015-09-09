fun foo() {
    bar()
}

fun bar() {
    foo()
}

var a : Int
  <caret>

// EXIST:  abstract
// EXIST: by
// EXIST:  class
// EXIST:  enum
// EXIST:  final
// EXIST:  fun
// EXIST: get
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
// EXIST: set
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
// EXIST:  crossinline
/*TODO*/
// NOTHING_ELSE
