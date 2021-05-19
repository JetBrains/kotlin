// FIR_COMPARISON
// COMPILER_ARGUMENTS: -XXLanguage:+SealedInterfaces -XXLanguage:+MultiPlatformProjects

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
// EXIST:  class GlobalPropertyAccessors
// EXIST:  enum class
// EXIST:  enum class GlobalPropertyAccessors
// EXIST:  final
// EXIST:  fun
// EXIST: { itemText: "get", tailText: null }
// EXIST: { itemText: "get", tailText: "() = ..." }
// EXIST: { itemText: "get", tailText: "() {...}" }
// EXIST:  internal
// EXIST:  object
// EXIST:  object GlobalPropertyAccessors
// EXIST:  open
// EXIST:  private
// EXIST:  public
// EXIST: { itemText: "set", tailText: null }
// EXIST: { itemText: "set", tailText: "(value) = ..." }
// EXIST: { itemText: "set", tailText: "(value) {...}" }
// EXIST:  interface
// EXIST:  interface GlobalPropertyAccessors
// EXIST:  val
// EXIST:  var
// EXIST:  operator
// EXIST:  infix
// EXIST:  sealed class
// EXIST:  sealed class GlobalPropertyAccessors
// EXIST:  sealed interface GlobalPropertyAccessors
// EXIST:  sealed interface
// EXIST:  data class
// EXIST:  { "lookupString":"data class", "itemText":"data class", "tailText":" GlobalPropertyAccessors(...)", "attributes":"bold" }
// EXIST:  inline
// EXIST:  value
// EXIST:  tailrec
// EXIST:  external
// EXIST:  annotation class
// EXIST:  annotation class GlobalPropertyAccessors
// EXIST:  const val
// EXIST:  suspend fun
// EXIST:  typealias
// EXIST:  expect
// EXIST:  actual
// EXIST:  lateinit var
// NOTHING_ELSE
