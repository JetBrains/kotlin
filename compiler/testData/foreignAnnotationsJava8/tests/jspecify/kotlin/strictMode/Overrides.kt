// JAVA_SOURCES: Overrides.java
// JSPECIFY_STATE strict

interface SubObjectKt : Overrides.Super {
    override fun makeObject(): Any
    override fun makeObjectUnspec(): Any
    override fun makeObjectUnionNull(): Any
}
interface SubObjectUnspecKt : Overrides.Super {
    // jspecify_nullness_not_enough_information
    override fun makeObject(): Any
    // jspecify_nullness_not_enough_information
    override fun makeObjectUnspec(): Any
    override fun makeObjectUnionNull(): Any
}
interface SubObjectUnionNullKt : Overrides.Super {
    // jspecify_nullness_mismatch
    override fun makeObject(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Any?<!>
    // jspecify_nullness_not_enough_information
    override fun makeObjectUnspec(): Any?
    override fun makeObjectUnionNull(): Any?
}
