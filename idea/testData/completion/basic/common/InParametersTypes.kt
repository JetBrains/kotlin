class SomeClass {
  class SomeInternal

  fun some(a : S<caret>)
}

// TIME: 1
// EXIST: SomeClass, SomeInternal
// EXIST: String~(jet)
// EXIST: IllegalStateException
// EXIST_JAVA_ONLY: StringBuilder
// EXIST_JAVA_ONLY: StringBuffer
// ABSENT: HTMLStyleElement
// ABSENT: Statement@Statement~(java.sql)