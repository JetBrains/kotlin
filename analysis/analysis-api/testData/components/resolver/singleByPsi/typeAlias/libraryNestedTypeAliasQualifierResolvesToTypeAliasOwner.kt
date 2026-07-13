// LANGUAGE: +NestedTypeAliases
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt

class ExpandedTypeOwner {
    class ExpandedNestedClass
}

class TypeAliasOwner {
    // The expanded target must be nested under a different owner to ensure the qualifier
    // resolves through the abbreviated type alias path, not the expanded class path.
    typealias NestedAlias = ExpandedTypeOwner.ExpandedNestedClass
}

// MODULE: main(lib)
// FILE: usage.kt

fun consume(parameter: <caret>TypeAliasOwner.NestedAlias) {}
