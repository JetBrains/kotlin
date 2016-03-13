// "Change 'foo' function return type to '([ERROR : NoSuchType]) -> Int'" "false"
// ACTION: Create annotation 'NoSuchType'
// ACTION: Create class 'NoSuchType'
// ACTION: Create enum 'NoSuchType'
// ACTION: Create interface 'NoSuchType'
// ACTION: Remove explicit lambda parameter types (may break code)
// ERROR: Type mismatch: inferred type is ([ERROR : NoSuchType]) -> Int but Int was expected
// ERROR: Unresolved reference: NoSuchType

fun foo(): Int {
    return { x: NoSuchType<caret> -> 42 }
}