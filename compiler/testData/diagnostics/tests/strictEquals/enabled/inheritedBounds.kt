// RUN_PIPELINE_TILL: FRONTEND

object Unrelated

open class Child {
    override fun equals(@EqualityBound(Child::class) other: Any?): Boolean = true
}

class ParentSilentInheritance : Child()

class ParentExplicitRegularEquals : Child() {
    override fun equals(other: Any?): Boolean = true
}

class ParentExplicitAnyRegularEquals : Child() {
    // EBT should be a subtype of child's EBT
    <!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE!>override fun equals(@EqualityBound(Any::class) other: Any?): Boolean = true<!>
}

fun test1(
    child: Child,
    parentSilentInheritance: ParentSilentInheritance,
    parentExplicitRegularEquals: ParentExplicitRegularEquals,
    parentExplicitAnyRegularEquals: ParentExplicitAnyRegularEquals,
) {
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>child == Unrelated<!>) {}

    if (child == parentSilentInheritance) {}
    if (child == parentExplicitRegularEquals) {}
    if (child == parentExplicitAnyRegularEquals) {}

    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>parentSilentInheritance == Unrelated<!>) {}
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>parentExplicitRegularEquals == Unrelated<!>) {}
    if (parentExplicitAnyRegularEquals == Unrelated) {} // that's OK as we have an error at the declaration
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, ifExpression,
nullableType, objectDeclaration, operator, override */
