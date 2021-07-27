open class Fun(
    protected val p1: String = "something"
        <!UNRESOLVED_REFERENCE!>public<!> <!UNRESOLVED_REFERENCE!>get<!>()<!SYNTAX!><!>: Any
)

open class C1(
    open protected val p1: String = "something"
) {
    open protected val p2 = "something"
        public get(): Any

    open protected val p3 = "something"
        public get(): Any

    open protected val p4 = "something"
        public get(): Any

    open protected val p5 = "something"
        public get(): Any

    open protected val p6 = "something"
        public get(): Any

    open protected val p7 = "something"
        public get(): Any

    open protected val p8 = "something"
        public get(): Any

    open protected val p9 = "something"
        public get(): Any
}

class C2(
    override val p1: String = "lol",
    <!INCOMPLETE_PROPERTY_OVERRIDE!>override val p2: String = "lol"<!>,
    public override val p5: String = "lol",
    <!INCOMPLETE_PROPERTY_OVERRIDE!>override val p7: String = "lol" <!UNRESOLVED_REFERENCE!>public<!> <!UNRESOLVED_REFERENCE!>get<!>()<!><!SYNTAX!><!>: Any,
) : C1() {
    <!INCOMPLETE_PROPERTY_OVERRIDE!>override val p3 get() = "lol"<!>
    <!INCOMPLETE_PROPERTY_OVERRIDE!>override val p4 = "lol"<!>
    override val p6 <!EXPOSING_GETTER_WITH_BODY!><!REDUNDANT_GETTER_VISIBILITY_CHANGE!>public<!> get() = "lol"<!>
    public override val p8 get() = "lol"

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>override val <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>p9<!><!> public get(): Any
}

fun check() {
    val it = C2("a", "b", "c", "d"<!NO_VALUE_FOR_PARAMETER!>)<!>
    println(it.<!INVISIBLE_REFERENCE!>p1<!>.length)
    println(it.<!INVISIBLE_REFERENCE!>p2<!>.length)
    println(it.<!INVISIBLE_REFERENCE!>p3<!>.length)
    println(it.<!INVISIBLE_REFERENCE!>p4<!>.length)
    println(it.p5.length)
    println(it.p6.length)
    println(it.<!INVISIBLE_REFERENCE!>p7<!>.length)
    println(it.p8.length)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(it.p9.<!UNRESOLVED_REFERENCE!>length<!>)
}


