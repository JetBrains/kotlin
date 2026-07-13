// LANGUAGE: +NestedTypeAliases

class ExpandedTypeOwner {
    class ExpandedNestedClass
}

class IntermediateAliasOwner {
    typealias IntermediateAlias = ExpandedTypeOwner.ExpandedNestedClass
}

class TypeAliasOwner {
    // The expanded target must be nested under a different owner to ensure the qualifier
    // resolves through the abbreviated type alias path, not the expanded class path.
    typealias NestedAlias = IntermediateAliasOwner.IntermediateAlias
}

fun consume(parameter: <caret>TypeAliasOwner.NestedAlias) {}
