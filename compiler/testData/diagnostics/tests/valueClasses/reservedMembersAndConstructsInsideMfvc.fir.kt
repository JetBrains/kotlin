// !SKIP_JAVAC
// !LANGUAGE: +CustomEqualsInValueClasses, +ValueClasses
// ALLOW_KOTLIN_PACKAGE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB

@JvmInline
value class IC1(val x: Any) {
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>(x: Any) {}

    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>(x: Any) {}

    override fun <!INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS!>equals<!>(other: Any?): Boolean = true
    override fun hashCode(): Int = 0
}

@JvmInline
value class IC2(val x: Any) {
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>(x: Any) {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>(): Any = TODO()

    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>(x: Any) {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>(): Any = TODO()

    fun equals(my: Any, other: Any): Boolean = true
    fun hashCode(a: Any): Int = 0
}

@JvmInline
value class IC3(val x: Any) {
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>(x: Any): Any = TODO()
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>(x: Any): Any = TODO()

    fun equals(): Boolean = true
}

interface WithBox {
    fun box(): String
}

@JvmInline
value class IC4(val s: String) : WithBox {
    override fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>(): String = ""
}

@JvmInline
value class IC5(val a: String) {
    constructor(i: Int) : this(i.toString()) <!SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS!>{
        TODO("something")
    }<!>
}

@JvmInline
@AllowTypedEquals
value class IC6(val a: String) {
    @TypedEquals
    fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> equals(other: IC6): Boolean = true
}

@JvmInline
value class MFVC1(val x: Any, val y: Any) {
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>(x: Any) {}

    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>() {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>(x: Any) {}

    override fun <!INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS!>equals<!>(other: Any?): Boolean = true
    override fun hashCode(): Int = 0
}

@JvmInline
value class MFVC2(val x: Any, val y: Any) {
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>(x: Any) {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>(): Any = TODO()

    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>(x: Any) {}
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>(): Any = TODO()

    fun equals(my: Any, other: Any): Boolean = true
    fun hashCode(a: Any): Int = 0
}

@JvmInline
value class MFVC3(val x: Any, val y: Any) {
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>(x: Any): Any = TODO()
    fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>unbox<!>(x: Any): Any = TODO()

    fun equals(): Boolean = true
}

@JvmInline
value class MFVC4(val s: String, val t: String) : WithBox {
    override fun <!RESERVED_MEMBER_INSIDE_VALUE_CLASS!>box<!>(): String = ""
}

@JvmInline
value class MFVC5(val a: String, val b: String) {
    constructor(i: Int) : this(i.toString(), "6") <!SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS!>{
        TODO("something")
    }<!>
}

@JvmInline
@AllowTypedEquals
value class MFVC6(val a: String, val b: String) {
    @TypedEquals
    fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> equals(other: MFVC6): Boolean = true
}

@JvmInline
value class MFVC7<T>(val a: String, val b: String) {
    fun equals(other: MFVC7<*>): Boolean = true
}

@JvmInline
value class MFVC8<T>(val a: String, val b: String) {
    <!INAPPLICABLE_TYPED_EQUALS_ANNOTATION!>@TypedEquals<!>
    fun equals(other: MFVC8<T>): Boolean = true
}

@JvmInline
value class MFVC9<T>(val a: String, val b: String) {
    <!INAPPLICABLE_TYPED_EQUALS_ANNOTATION!>@TypedEquals<!>
    fun equals(other: MFVC9<String>): Boolean = true
}

@JvmInline
@AllowTypedEquals
value class MFVC10(val a : String, val b: String) {
    @TypedEquals
    fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> equals(other: MFVC10): Boolean = true
}