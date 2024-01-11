// IGNORE_TREE_ACCESS: KT-64898
@Anno("Derived $x")
data class Derived @Anno("Derived constructor $x") constructor(
    @Anno("b $x")
    @get:Anno("get: b $x")
    @param:Anno("param: b $x")
    @property:Anno("property: b $x")
    val b: @Anno("Derived b parameter type $x") B<@Anno("nested Derived b parameter type $x") BNested<@Anno("nested nested Derived b parameter type $x") BNestedNested>>,
    val c: @Anno("Derived c parameter type $x") C<@Anno("nested Derived c parameter type $x") CNested<@Anno("nested nested Derived c parameter type $x") CNestedNested>>,
) : @Anno("Base super type call $x") Base<@Anno("nested super type call $x") Nested<@Anno("nested nested super type call $x") NestedNested>>()
