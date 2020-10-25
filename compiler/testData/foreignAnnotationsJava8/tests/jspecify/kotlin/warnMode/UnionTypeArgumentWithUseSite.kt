// JAVA_SOURCES: UnionTypeArgumentWithUseSite.java

interface SubKt : UnionTypeArgumentWithUseSite.Super<Any> {
    override fun t(t: Any): Unit
    // jspecify_nullness_not_enough_information
    override fun tUnspec(t: Any): Unit
    override fun tUnionNull(t: Any?): Unit
}
interface SubUnspecKt : UnionTypeArgumentWithUseSite.Super<Any> {
    // jspecify_nullness_not_enough_information
    override fun t(t: Any): Unit
    // jspecify_nullness_not_enough_information
    override fun tUnspec(t: Any): Unit
    override fun tUnionNull(t: Any?): Unit
}
interface SubUnionNullKt : UnionTypeArgumentWithUseSite.Super<Any?> {
    override fun t(t: Any?): Unit
    override fun tUnspec(t: Any?): Unit
    override fun tUnionNull(t: Any?): Unit
}
interface SubWeakerKt : UnionTypeArgumentWithUseSite.Super<Any> {
    // jspecify_nullness_not_enough_information
    override fun tUnspec(t: Any): Unit
    // jspecify_nullness_mismatch
    override fun tUnionNull(t: Any): Unit
}
interface SubWeakerUnspecKt : UnionTypeArgumentWithUseSite.Super<Any> {
    // jspecify_nullness_not_enough_information
    override fun t(t: Any): Unit
    // jspecify_nullness_not_enough_information
    override fun tUnspec(t: Any): Unit
    // jspecify_nullness_mismatch
    override fun tUnionNull(t: Any): Unit
}
interface SubWeakerUnionNullKt : UnionTypeArgumentWithUseSite.Super<Any?> {
    // jspecify_nullness_mismatch
    <!NOTHING_TO_OVERRIDE!>override<!> fun t(t: Any): Unit
    // jspecify_nullness_mismatch
    <!NOTHING_TO_OVERRIDE!>override<!> fun tUnspec(t: Any): Unit
    // jspecify_nullness_mismatch
    <!NOTHING_TO_OVERRIDE!>override<!> fun tUnionNull(t: Any): Unit
}
