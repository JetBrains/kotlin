// WITH_STDLIB
// ISSUE: KT-63221
// SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK: KT-63221

// Property initializers are a part of the control flow graph of the containing file or class, so control flow diagnostics reported inside
// them belong to the file or the class structure element and not to the structure element of the property.
val topLevel: Int = run {
    error("First")
    error("Second")
}

class Foo {
    val member: Int = run {
        error("First")
        error("Second")
    }
}
