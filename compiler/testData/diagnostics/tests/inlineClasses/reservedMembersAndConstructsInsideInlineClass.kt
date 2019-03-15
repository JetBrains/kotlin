// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline class IC1(val x: Any) {
    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>box<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>box<!>(x: Any) {}

    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>unbox<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>unbox<!>(x: Any) {}

    override fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>equals<!>(other: Any?): Boolean = true
    override fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>hashCode<!>(): Int = 0
}

inline class IC2(val x: Any) {
    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>box<!>(x: Any) {}
    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>box<!>(): Any = TODO()

    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>unbox<!>(x: Any) {}
    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>unbox<!>(): Any = TODO()

    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>equals<!>(my: Any, other: Any): Boolean = true
    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>hashCode<!>(a: Any): Int = 0
}

inline class IC3(val x: Any) {
    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>box<!>(x: Any): Any = TODO()
    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>unbox<!>(x: Any): Any = TODO()

    fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>equals<!>(): Boolean = true
}

interface WithBox {
    fun box(): String
}

inline class IC4(val s: String) : WithBox {
    override fun <!RESERVED_MEMBER_INSIDE_INLINE_CLASS!>box<!>(): String = ""
}

inline class IC5(val a: String) {
    constructor(i: Int) : this(i.toString()) <!SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS!>{<!>
        TODO("something")
    }
}