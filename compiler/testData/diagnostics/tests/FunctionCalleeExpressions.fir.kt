// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package foo

fun Any.foo() : () -> Unit {
  return {}
}

fun Any.foo1() : (i : Int) -> Unit {
  return {}
}

fun foo2() : (i : () -> Unit) -> Unit {
  return {}
}

fun <T> fooT1(t : T) : () -> T {
  return {t}
}

fun <T> fooT2() : (t : T) -> T {
  return {it}
}

fun main(args : Array<String>) {
    args.foo()()
    <!INAPPLICABLE_CANDIDATE!>args.foo1()()<!>
    <!UNRESOLVED_REFERENCE!>a<!>.foo1()()
    <!UNRESOLVED_REFERENCE!>a<!>.foo1()(<!UNRESOLVED_REFERENCE!>a<!>)

    args.foo1()(1)
    <!INAPPLICABLE_CANDIDATE!>args.foo1()("1")<!>
    <!INAPPLICABLE_CANDIDATE!><!UNRESOLVED_REFERENCE!>a<!>.foo1()("1")<!>
    <!UNRESOLVED_REFERENCE!>a<!>.foo1()(<!UNRESOLVED_REFERENCE!>a<!>)

    foo2()({})
    <!INAPPLICABLE_CANDIDATE!>foo2<!>(){}
    (foo2()){}
    <!INAPPLICABLE_CANDIDATE!>(foo2()){x -> }<!>
    <!INAPPLICABLE_CANDIDATE!>foo2()({x -> })<!>

    val a = fooT1(1)()
    checkSubtype<Int>(a)

    val b = fooT2<Int>()(1)
    checkSubtype<Int>(b)
    fooT2()(1) // : Any?

    <!UNRESOLVED_REFERENCE!>1()<!>
    <!UNRESOLVED_REFERENCE!>1{}<!>
    <!UNRESOLVED_REFERENCE!>1(){}<!>
}

fun f() :  Int.() -> Unit = {}

fun main1() {
    1.<!UNRESOLVED_REFERENCE!>(fun Int.() = 1)()<!>;
    <!UNRESOLVED_REFERENCE!>{1}()<!>;
    <!UNRESOLVED_REFERENCE!>(fun (x : Int) = x)(1)<!>
    1.<!UNRESOLVED_REFERENCE!>(fun Int.(x : Int) = x)(1)<!>;
    l@<!UNRESOLVED_REFERENCE!>{1}()<!>
    1.<!UNRESOLVED_REFERENCE!>((fun Int.() = 1))()<!>
    1.<!UNRESOLVED_REFERENCE!>(f())()<!>
    1.<!UNRESOLVED_REFERENCE!>if(true){f()}else{f()}()<!>
    1.<!UNRESOLVED_REFERENCE!>if(true)(fun Int.() {})else{f()}()<!>

    1.<!UNRESOLVED_REFERENCE!>"sdf"()<!>

    1."sdf"
    1.{}
    1.if (true) {}
}

fun test() {
    <!UNRESOLVED_REFERENCE!>{x : Int -> 1}()<!>;
    <!UNRESOLVED_REFERENCE!>(fun Int.() = 1)()<!>
    "sd".<!UNRESOLVED_REFERENCE!>(fun Int.() = 1)()<!>
    val i : Int? = null
    i.<!UNRESOLVED_REFERENCE!>(fun Int.() = 1)()<!>;
    <!UNRESOLVED_REFERENCE!>{}<Int>()<!>
    1?.<!UNRESOLVED_REFERENCE!>(fun Int.() = 1)()<!>
    1.<!UNRESOLVED_REFERENCE!>{}()<!>
}
