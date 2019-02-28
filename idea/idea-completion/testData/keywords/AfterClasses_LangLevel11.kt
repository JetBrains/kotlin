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

<caret>

// EXIST:  abstract
// EXIST:  class
// EXIST:  class AfterClasses_LangLevel11
// EXIST:  enum class
// EXIST:  enum class AfterClasses_LangLevel11
// EXIST:  final
// EXIST:  fun
// EXIST:  internal
// EXIST:  object
// EXIST:  object AfterClasses_LangLevel11
// EXIST:  open
// EXIST:  private
// EXIST:  public
// EXIST:  interface
// EXIST:  interface AfterClasses_LangLevel11
// EXIST:  val
// EXIST:  var
// EXIST:  operator
// EXIST:  infix
// EXIST:  sealed class
// EXIST:  sealed class AfterClasses_LangLevel11
// EXIST:  data class
// EXIST:  data class AfterClasses_LangLevel11
// EXIST:  inline
// EXIST:  tailrec
// EXIST:  external
// EXIST:  annotation class
// EXIST:  annotation class AfterClasses_LangLevel11
// EXIST:  const val
// EXIST:  suspend fun
// EXIST:  typealias
// NOTHING_ELSE
