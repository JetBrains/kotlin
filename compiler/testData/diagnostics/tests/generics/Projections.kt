class In<in T>() {
    fun f(<!UNUSED_PARAMETER!>t<!> : T) : Unit {}
    fun f(<!UNUSED_PARAMETER!>t<!> : Int) : Int = 1
    fun f1(<!UNUSED_PARAMETER!>t<!> : T) : Unit {}
}

class Out<out T>() {
    fun f() : T {throw IllegalStateException()}
    fun f(a : Int) : Int = a
}

class Inv<T>() {
    fun f(t : T) : T = t
    fun inf(<!UNUSED_PARAMETER!>t<!> : T) : Unit {}
    fun outf() : T {throw IllegalStateException()}
}

fun testInOut() {
    In<String>().f("1");
    (null <!CAST_NEVER_SUCCEEDS!>as<!> In<in String>).f("1")
    (null <!CAST_NEVER_SUCCEEDS!>as<!> In<out String>).f(<!TYPE_MISMATCH!>"1"<!>) // Wrong Arg
    (null <!CAST_NEVER_SUCCEEDS!>as<!> In<*>).f(<!TYPE_MISMATCH!>"1"<!>) // Wrong Arg

    In<String>().f(1);
    (null <!CAST_NEVER_SUCCEEDS!>as<!> In<in String>).f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> In<out String>).f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> In<out String>).<!UNRESOLVED_REFERENCE!>f1<!>(1) // !!
    (null <!CAST_NEVER_SUCCEEDS!>as<!> In<*>).f(1);

    Out<Int>().f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Out<out Int>).f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Out<in Int>).f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Out<*>).f(1)

    Out<Int>().f()
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Out<out Int>).f()
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Out<in Int>).f()
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Out<*>).f()

    Inv<Int>().f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<in Int>).f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<out Int>).<!UNRESOLVED_REFERENCE!>f<!>(1) // !!
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<*>).<!UNRESOLVED_REFERENCE!>f<!>(1) // !!

    Inv<Int>().inf(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<in Int>).inf(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<out Int>).<!UNRESOLVED_REFERENCE!>inf<!>(1) // !!
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<*>).<!UNRESOLVED_REFERENCE!>inf<!>(1) // !!

    Inv<Int>().outf()
    <!TYPE_MISMATCH!>(null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<in Int>).outf()<!> :  Int // Type mismatch
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<out Int>).outf()
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<*>).outf()

    Inv<Int>().outf(<!TOO_MANY_ARGUMENTS!>1<!>) // Wrong Arg
}