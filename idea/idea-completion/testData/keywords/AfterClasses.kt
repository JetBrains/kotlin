// FIR_IDENTICAL
// FIR_COMPARISON
// COMPILER_ARGUMENTS: -XXLanguage:+SealedInterfaces -XXLanguage:+MultiPlatformProjects

class A {
    fun foo() {
        bar()
    }
}

class B {
    fun bar() {
        foo()
    }
}

class AfterClasses {
}

<caret>

// EXIST:  abstract
// EXIST:  class
// EXIST:  enum class
// EXIST:  final
// EXIST:  fun
// EXIST:  internal
// EXIST:  object
// EXIST:  open
// EXIST:  private
// EXIST:  public
// EXIST:  interface
// EXIST:  val
// EXIST:  var
// EXIST:  operator
// EXIST:  infix
// EXIST:  sealed class
// EXIST:  sealed interface
// EXIST:  data class
// EXIST:  inline
// EXIST:  value
// EXIST:  tailrec
// EXIST:  external
// EXIST:  annotation class
// EXIST:  const val
// EXIST:  suspend fun
// EXIST:  typealias
// EXIST:  expect
// EXIST:  actual
// EXIST:  lateinit var
// NOTHING_ELSE
