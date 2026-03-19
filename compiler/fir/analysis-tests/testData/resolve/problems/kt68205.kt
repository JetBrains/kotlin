// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-68205

// KT-68205: Consider other approaches for handling ILT types in ResultTypeResolver

fun <T> id(x: T): T = x

fun <T> same(a: T, b: T): T = a

fun test() {
    // Simple ILT - equality constraint from literal, no direction -> should infer Int
    val a = id(1)

    // ILT with direction - equality constraint from literal, direction = Long
    val b: Long = id(1)

    // Both args same ILT literal - equality constraint from both
    val c = same(1, 2)

    // Both ILT literals with direction = Long
    val d: Long = same(1, 2)

    // Mixed: ILT and concrete Long - equality constraint with ILT and Long
    val e = same(1, 2L)

    // Nested: ILT in generic position
    val f = id(id(1))

    // ILT with Byte direction
    val g: Byte = id(1)

    // ILT with Short direction
    val h: Short = id(1)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, nullableType, propertyDeclaration,
typeParameter */
