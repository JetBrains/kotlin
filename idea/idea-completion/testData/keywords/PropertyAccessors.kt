// FIR_COMPARISON
// COMPILER_ARGUMENTS: -XXLanguage:+SealedInterfaces -XXLanguage:+MultiPlatformProjects

class Some {
    var a : Int
        <caret>
}

// EXIST:  abstract
// EXIST: by
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
// EXIST:  companion object
// EXIST:  operator
// EXIST:  infix
// EXIST:  sealed class
// EXIST:  sealed interface
// EXIST:  lateinit var
// EXIST:  data class
// EXIST:  inline
// EXIST:  value
// EXIST:  tailrec
// EXIST:  external
// EXIST:  annotation class
// EXIST:  suspend fun
// EXIST:  typealias
// EXIST:  expect
// EXIST:  actual
// NOTHING_ELSE
