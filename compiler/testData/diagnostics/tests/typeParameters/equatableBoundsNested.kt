// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

class Box<T>(val value: T)

fun <S, T> sameBox(s: Box<S>, t: Box<T>) where S == T {}

fun testNestedSameType() {
    sameBox(Box(1), Box(2))
}

fun testNestedDifferentTypes() {
    <!EQUATABLE_TYPE_BOUND_VIOLATED, EQUATABLE_TYPE_BOUND_VIOLATED!>sameBox<!>(Box(1), Box("hello"))
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, primaryConstructor,
propertyDeclaration, stringLiteral, typeParameter */
