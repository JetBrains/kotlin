class Test {
    val St<caret>
}

// INVOCATION_COUNT: 1
// EXIST: String~(jet)
// EXIST: IllegalStateException
// EXIST: StringBuilder
// EXIST_JAVA_ONLY: StringBuffer
// ABSENT: HTMLStyleElement
// ABSENT: Statement@Statement~(java.sql)
