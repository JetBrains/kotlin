// !LANGUAGE: +CustomEqualsInValueClasses, +ValueClasses

<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class BackingFields(val x: Int) {
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val y<!> = 0
    var z: String
        get() = ""
        set(value) {}
}

class Val {
    operator fun getValue(thisRef: Any?, kProp: Any?) = 1
}
inline class DelegatedProp(val x: Int) {
    val testVal by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>Val()<!>
}

inline class ReservedMembers(val x: Int) {
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>() {}

    override fun <!INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS!>equals<!>(other: Any?) = true
    override fun hashCode() = 1
}

inline class ReservedMembersMfvc(val x: Int, val y: Int) {
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>() {}

    override fun <!INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS!>equals<!>(other: Any?) = true
    override fun hashCode() = 1
}

inline class SecondaryConstructors(val x: Int) {
    constructor(y: String) : this(5)
    constructor(x: Int, y: String) : this(x) {

    }
}

<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class WithInner(val x: String) {
    <!INNER_CLASS_INSIDE_VALUE_CLASS!>inner<!> class Inner
}
