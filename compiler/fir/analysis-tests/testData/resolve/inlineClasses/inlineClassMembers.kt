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
    <!RESERVED_MEMBER_INSIDE_INLINE_CLASS{LT}!>fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS{PSI}!>box<!>() {}<!>
    <!RESERVED_MEMBER_INSIDE_INLINE_CLASS{LT}!>fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS{PSI}!>unbox<!>() {}<!>

    <!RESERVED_MEMBER_INSIDE_INLINE_CLASS{LT}!>override fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS{PSI}!>equals<!>(other: Any?) = true<!>
    <!RESERVED_MEMBER_INSIDE_INLINE_CLASS{LT}!>override fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS{PSI}!>hashCode<!>() = 1<!>
}

inline class SecondaryConstructors(val x: Int) {
    constructor(y: String) : this(5)
    constructor(x: Int, y: String) : this(x) <!SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS!>{

    }<!>
}

value class WithInner(val x: String) {
    <!INNER_CLASS_INSIDE_INLINE_CLASS!>inner<!> class Inner
}
