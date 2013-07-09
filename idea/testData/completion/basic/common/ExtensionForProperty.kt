class Test {
    val St<caret>
}

// TIME: 1
// EXIST: String~(jet)
// EXIST: IllegalStateException
// EXIST_JAVA_ONLY: StringBuilder
// EXIST_JAVA_ONLY: StringBuffer
// ABSENT: HTMLStyleElement
// ABSENT: Statement@Statement~(java.sql)
