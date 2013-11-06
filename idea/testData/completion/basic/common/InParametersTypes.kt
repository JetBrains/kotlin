class SomeClass {
  class SomeInternal

  fun some(a : S<caret>)
}

// INVOCATION_COUNT: 1
// EXIST: SomeClass, SomeInternal
// EXIST: String~(jet)
// EXIST: IllegalStateException
// EXIST: StringBuilder
// EXIST_JAVA_ONLY: StringBuffer
// ABSENT: HTMLStyleElement
// ABSENT: Statement@Statement~(java.sql)