// LANGUAGE: +NestedTypeAliases

class ExpandedTypeOwner {
    class ExpandedNestedClass
}

class Outer {
    class TypeAliasOwner {
        // The expanded target must be nested under a different owner to ensure the qualifier
        // resolves through the abbreviated type alias path, not the expanded class path.
        typealias NestedAlias = ExpandedTypeOwner.ExpandedNestedClass
    }
}

fun consume(parameter: <caret>Outer.TypeAliasOwner.NestedAlias) {}
