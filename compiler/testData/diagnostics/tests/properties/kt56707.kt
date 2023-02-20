// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-56707

class Foo {
    val children = mutableSetOf<Foo>()
    val allChildren
        get() : Set<Foo> = (children + children.flatMap { it.allChildren }).toSet() // Should not be TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM on `allChildren` reference
}

fun use() {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Set<Foo>")!>Foo().allChildren<!>
}
