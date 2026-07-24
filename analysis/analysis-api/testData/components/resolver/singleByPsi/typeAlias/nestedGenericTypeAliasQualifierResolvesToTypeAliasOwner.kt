// LANGUAGE: +NestedTypeAliases

class ExpandedTypeOwner {
    class ExpandedNestedClass<T>
}

class TypeAliasOwner {
    // The expanded target must be nested under a different owner to ensure the qualifier
    // resolves through the abbreviated type alias path, not the expanded class path.
    typealias NestedAlias<T> = ExpandedTypeOwner.ExpandedNestedClass<T>
}

fun consume(parameter: <caret>TypeAliasOwner.NestedAlias<String>) {}
