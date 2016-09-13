// "Change 'foo' function return type to '(x: [ERROR : NoSuchType]) -> Int'" "false"
// ACTION: Create annotation 'NoSuchType'
// ACTION: Create class 'NoSuchType'
// ACTION: Create enum 'NoSuchType'
// ACTION: Create interface 'NoSuchType'
// ACTION: Create type alias 'NoSuchType'
// ACTION: Remove explicit lambda parameter types (may break code)
// ACTION: Create type parameter 'NoSuchType' in function 'foo'
// ERROR: Type mismatch: inferred type is (x: [ERROR : NoSuchType]) -> Int but Int was expected
// ERROR: Unresolved reference: NoSuchType

fun foo(): Int {
    return { x: NoSuchType<caret> -> 42 }
}