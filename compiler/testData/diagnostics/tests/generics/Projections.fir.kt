// !CHECK_TYPE

class In<in T>() {
    fun f(t : T) : Unit {}
    fun f(t : Int) : Int = 1
    fun f1(t : T) : Unit {}
}

class Out<out T>() {
    fun f() : T {throw IllegalStateException()}
    fun f(a : Int) : Int = a
}

class Inv<T>() {
    fun f(t : T) : T = t
    fun inf(t : T) : Unit {}
    fun outf() : T {throw IllegalStateException()}
}

fun testInOut() {
    In<String>().f("1");
    (null <!CAST_NEVER_SUCCEEDS!>as<!> In<in String>).f("1")
    (null <!CAST_NEVER_SUCCEEDS!>as<!> In<*>).<!NONE_APPLICABLE!>f<!>("1") // Wrong Arg

    In<String>().f(1);
    (null <!CAST_NEVER_SUCCEEDS!>as<!> In<in String>).f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> In<*>).f(1);

    Out<Int>().f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Out<out Int>).f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Out<*>).f(1)

    Out<Int>().f()
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Out<out Int>).f()
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Out<*>).f()

    Inv<Int>().f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<in Int>).f(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<out Int>).f(<!ARGUMENT_TYPE_MISMATCH!>1<!>) // !!
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<*>).f(<!ARGUMENT_TYPE_MISMATCH!>1<!>) // !!

    Inv<Int>().inf(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<in Int>).inf(1)
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<out Int>).inf(<!ARGUMENT_TYPE_MISMATCH!>1<!>) // !!
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<*>).inf(<!ARGUMENT_TYPE_MISMATCH!>1<!>) // !!

    Inv<Int>().outf()
    checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>(null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<in Int>).outf()<!>) // Type mismatch
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<out Int>).outf()
    (null <!CAST_NEVER_SUCCEEDS!>as<!> Inv<*>).outf()

    Inv<Int>().outf(<!TOO_MANY_ARGUMENTS!>1<!>) // Wrong Arg
}
