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
// EXIST:  enum class
// EXIST:  final
// EXIST:  fun
// EXIST: get
// EXIST: "get() = "
// EXIST: "get() {}"
// EXIST:  internal
// EXIST:  object
// EXIST:  open
// EXIST:  private
// EXIST:  public
// EXIST: set
// EXIST: "set(value) = "
// EXIST: "set(value) {}"
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
// EXIST:  annotation class
// EXIST:  const
// NOTHING_ELSE
