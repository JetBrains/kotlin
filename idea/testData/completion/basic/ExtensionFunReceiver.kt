fun testing() {}

fun S<caret>

// Should complete types for receiver after explicit basic completion call
// TIME: 1
// EXIST: String
// EXIST_JAVA_ONLY: StringBuffer
// EXIST: Set
// ABSENT: testing

