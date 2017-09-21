class Some {
    var a : Int = 1
        <caret>
}

// EXIST:  abstract
// EXIST:  as
// EXIST:  class
// EXIST:  enum class
// EXIST:  final
// EXIST:  fun
// EXIST: { itemText: "get", tailText: null }
// EXIST: { itemText: "get", tailText: "() = ..." }
// EXIST: { itemText: "get", tailText: "() {...}" }
// EXIST:  inner
// EXIST:  internal
// EXIST:  object
// EXIST:  open
// EXIST:  override
// EXIST:  private
// EXIST:  protected
// EXIST:  public
// EXIST: { itemText: "set", tailText: null }
// EXIST: { itemText: "set", tailText: "(value) = ..." }
// EXIST: { itemText: "set", tailText: "(value) {...}" }
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
