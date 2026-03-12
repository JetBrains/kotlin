// LANGUAGE: +ValueClasses
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

abstract value class AbstractValue(val p0: Int, p1: Int) {
    abstract val p2: Int
    open <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val p3: Int<!> = 0
    open val p4: Int get() = 0
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val p5: Int<!> = 0
    val p6: Int get() = 0
    @JvmField
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val p7: Int<!> = 0
    @JvmField
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val p8: Int<!> = 0
    val p9 by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>lazy { 0 }<!>
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>var p10: Int<!> = 0
        get() = field
        set(value) { field = value }
}

sealed value class SealedValue(val s0: Int, val s1: Int, val s2: Int, val s3: Int) : AbstractValue(s0, s1) {
    abstract val s4: Int
    open <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val s5: Int<!> = 0
    open val s6: Int get() = 0
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val s7: Int<!> = 0
    val s8: Int get() = 0
    @JvmField
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val s9: Int<!> = 0
    @JvmField
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val s10: Int<!> = 0
    val s11 by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>lazy { 0 }<!>
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>var s12: Int<!> = 0
        get() = field
        set(value) { field = value }
}

sealed class SealedIdentity(val s0: Int, val s1: Int, val s2: Int, val s3: Int) : AbstractValue(s0, s1) {
    abstract val s4: Int
    open val s5: Int = 0
    open val s6: Int get() = 0
    val s7: Int = 0
    val s8: Int get() = 0
    @JvmField
    val s9: Int = 0
    @JvmField
    val s10: Int = 0
    val s11 by lazy { 0 }
    var s12: Int = 0
        get() = field
        set(value) { field = value }
}

<!VALUE_CLASS_OPEN!>open<!> value class OpenValue(val t0: Int, val t1: Int, val t2: Int, val t3: Int, val t4: Int, val t5: Int) : SealedValue(t0, t1, t2, t3) {
    override <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val p2<!> = 0
    override val s4 get() = 0
    open <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val t6: Int<!> = 0
    open val t7: Int get() = 0
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val t8: Int<!> = 0
    val t9: Int get() = 0
    @JvmField
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val t10: Int<!> = 0
    @JvmField
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val t11: Int<!> = 0
    val t12 by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>lazy { 0 }<!>
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>var t13: Int<!> = 0
        get() = field
        set(value) { field = value }
}

open class OpenIdentity(val t0: Int, val t1: Int, val t2: Int, val t3: Int, val t4: Int, val t5: Int) : SealedValue(t0, t1, t2, t3) {
    override val p2 = 0
    override val s4 get() = 0
    open val t6: Int = 0
    open val t7: Int get() = 0
    val t8: Int = 0
    val t9: Int get() = 0
    @JvmField
    val t10: Int = 0
    @JvmField
    val t11: Int = 0
    val t12 by lazy { 0 }
    var t13: Int = 0
        get() = field
        set(value) { field = value }
}

value class FinalValue(val t0: Int, val t1: Int, val t2: Int, val t3: Int, val t4: Int, val t5: Int) : SealedValue(t0, t1, t2, t3) {
    override <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val p2<!> = 0
    override val s4 get() = 0
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val t6: Int<!> = 0
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val t7: Int get() = 0
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val t8: Int<!> = 0
    val t9: Int get() = 0
    @JvmField
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val t10: Int<!> = 0
    @JvmField
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val t11: Int<!> = 0
    val t12 by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>lazy { 0 }<!>
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>var t13: Int<!> = 0
        get() = field
        set(value) { field = value }
}

class FinalIdentity(val t0: Int, val t1: Int, val t2: Int, val t3: Int, val t4: Int, val t5: Int) : SealedValue(t0, t1, t2, t3) {
    override val p2 = 0
    override val s4 get() = 0
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val t6: Int = 0
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val t7: Int get() = 0
    val t8: Int = 0
    val t9: Int get() = 0
    @JvmField
    val t10: Int = 0
    @JvmField
    val t11: Int = 0
    val t12 by lazy { 0 }
    var t13: Int = 0
        get() = field
        set(value) { field = value }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, getter, integerLiteral, lambdaLiteral, override, primaryConstructor,
propertyDeclaration, propertyDelegate, sealed, setter, value */
