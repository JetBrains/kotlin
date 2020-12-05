enum class Test {
    ;

    <caret>
}

// EXIST:  abstract
// EXIST:  enum class
// EXIST:  final
// EXIST:  inner
// EXIST:  internal
// EXIST:  open
// EXIST:  override
// EXIST:  private
// EXIST:  protected
// EXIST:  public
// EXIST:  companion object
// EXIST:  operator
// EXIST:  infix
// EXIST:  sealed class
// EXIST:  lateinit var
// EXIST:  data class
// EXIST:  inline
// EXIST:  value
// EXIST:  tailrec
// EXIST:  external
// EXIST:  annotation class
// EXIST:  suspend fun
// EXIST: fun
// EXIST:  typealias
// EXIST:  expect
// EXIST:  actual

/* TODO: items below are not valid here */
// EXIST: class
// EXIST: constructor
// EXIST: init
// EXIST: interface
// EXIST: object
// EXIST: val
// EXIST: var

// NOTHING_ELSE
