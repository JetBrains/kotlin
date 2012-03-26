fun testing() {}

fun S<caret>

// Should complete types for receiver after explicit basic completion call
// TIME: 1
// EXIST: String
// EXIST: StringBuffer
// EXIST: Set
// ABSENT: testing

