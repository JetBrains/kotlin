// FIR_IDENTICAL
// FIR_COMPARISON
// COMPILER_ARGUMENTS: -XXLanguage:+SealedInterfaces -XXLanguage:+MultiPlatformProjects

public class Test {

    <caret>

    fun test() {

    }
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
