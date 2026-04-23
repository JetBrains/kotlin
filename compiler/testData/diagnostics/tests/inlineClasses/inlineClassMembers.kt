// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CustomEqualsInValueClasses, +JvmInlineMultiFieldValueClasses

<!UNSUPPORTED_FEATURE!>value<!> class BackingFields(val x: Int) {
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val y<!> = 0
    var z: String
        get() = ""
        set(value) {}
}

class Val {
    operator fun getValue(thisRef: Any?, kProp: Any?) = 1
}
<!INLINE_CLASS_DEPRECATED!>inline<!> class DelegatedProp(val x: Int) {
    val testVal by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>Val()<!>
}

<!INLINE_CLASS_DEPRECATED!>inline<!> class ReservedMembers(val x: Int) {
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>() {}

    override fun <!INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS!>equals<!>(other: Any?) = true
    override fun hashCode() = 1
}

<!INLINE_CLASS_DEPRECATED!>inline<!> class ReservedMembersMfvc(val x: Int, val y: Int) {
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>() {}

    override fun <!INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS!>equals<!>(other: Any?) = true
    override fun hashCode() = 1
}

<!INLINE_CLASS_DEPRECATED!>inline<!> class SecondaryConstructors(val x: Int) {
    constructor(y: String) : this(5)
    constructor(x: Int, y: String) : this(x) {

    }
}

<!UNSUPPORTED_FEATURE("The feature \"full value classes\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-XXLanguage:+FullValueClasses', but note that no stability guarantees are provided.")!>value<!> class WithInner(val x: String) {
    <!INNER_CLASS_INSIDE_VALUE_CLASS!>inner<!> class Inner
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, inner, integerLiteral, nullableType, operator,
override, primaryConstructor, propertyDeclaration, propertyDelegate, secondaryConstructor, setter, stringLiteral, value */
