// JAVA_SOURCES: NotNullAwareOverrides.java
// JSPECIFY_STATE strict

interface SubObjectKt : NotNullAwareOverrides.Super {
    // jspecify_nullness_not_enough_information
    override fun makeObject(): Any
    // jspecify_nullness_not_enough_information
    override fun makeObjectUnspec(): Any
    override fun makeObjectUnionNull(): Any
}
interface SubObjectUnspecKt : NotNullAwareOverrides.Super {
    // jspecify_nullness_not_enough_information
    override fun makeObject(): Any
    // jspecify_nullness_not_enough_information
    override fun makeObjectUnspec(): Any
    override fun makeObjectUnionNull(): Any
}
interface SubObjectUnionNullKt : NotNullAwareOverrides.Super {
    // jspecify_nullness_not_enough_information
    override fun makeObject(): Any?
    // jspecify_nullness_not_enough_information
    override fun makeObjectUnspec(): Any?
    override fun makeObjectUnionNull(): Any?
}
