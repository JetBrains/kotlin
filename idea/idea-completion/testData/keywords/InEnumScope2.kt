enum class Test {
    ;

    <caret>
}

// EXIST:  abstract
// EXIST:  enum
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
// EXIST:  sealed
// EXIST:  lateinit
// EXIST:  data
// EXIST:  inline
// EXIST:  tailrec
// EXIST:  external
// EXIST:  annotation
// EXIST:  const
// EXIST: fun

/* TODO: items below are not valid here */
// EXIST: class
// EXIST: constructor
// EXIST: init
// EXIST: interface
// EXIST: object
// EXIST: val
// EXIST: var

// NOTHING_ELSE
