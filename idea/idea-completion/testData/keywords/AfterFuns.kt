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
// EXIST:  class
// EXIST:  enum class
// EXIST:  final
// EXIST:  fun
// EXIST:  inner
// EXIST:  internal
// EXIST:  object
// EXIST:  open
// EXIST:  override
// EXIST:  private
// EXIST:  protected
// EXIST:  public
// EXIST:  interface
// EXIST:  val
// EXIST:  var
// EXIST:  constructor
// EXIST:  init
// EXIST: { itemText: "companion object", tailText: " {...}" }
// EXIST:  operator
// EXIST:  infix
// EXIST:  sealed class
// EXIST:  lateinit var
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
