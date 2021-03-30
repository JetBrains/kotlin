value class BackingFields(val x: Int) {
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_INLINE_CLASS!>val y<!> = 0
    var z: String
        get() = ""
        set(value) {}
}

class Val {
    operator fun getValue(thisRef: Any?, kProp: Any?) = 1
}
inline class DelegatedProp(val x: Int) {
    val testVal by <!DELEGATED_PROPERTY_INSIDE_INLINE_CLASS!>Val()<!>
}

inline class ReversedMembers(val x: Int) {
    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>box<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>unbox<!>() {}

    override fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>equals<!>(other: Any?) = true
    override fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>hashCode<!>() = 1
}

inline class SecondaryConstructors(val x: Int) {
    constructor(y: String) : this(5)
    constructor(x: Int, y: String) : this(x) <!SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS!>{

    }<!>
}

value class WithInner(val x: String) {
    <!INNER_CLASS_INSIDE_INLINE_CLASS!>inner<!> class Inner
}
