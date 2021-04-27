// !WITH_NEW_INFERENCE
// !CHECK_TYPE

package foo

import checkType
import checkSubtype

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
    args.foo1(<!NO_VALUE_FOR_PARAMETER{PSI}!>)<!>(<!NO_VALUE_FOR_PARAMETER{LT}!>)<!>
    <!UNRESOLVED_REFERENCE!>a<!>.foo1(<!NO_VALUE_FOR_PARAMETER{PSI}!>)<!>(<!NO_VALUE_FOR_PARAMETER{LT}!>)<!>
    <!UNRESOLVED_REFERENCE!>a<!>.foo1()(<!UNRESOLVED_REFERENCE!>a<!>)

    args.foo1()(1)
    args.foo1()(<!ARGUMENT_TYPE_MISMATCH!>"1"<!>)
    <!UNRESOLVED_REFERENCE!>a<!>.foo1()(<!ARGUMENT_TYPE_MISMATCH!>"1"<!>)
    <!UNRESOLVED_REFERENCE!>a<!>.foo1()(<!UNRESOLVED_REFERENCE!>a<!>)

    foo2()({})
    foo2()<!TOO_MANY_ARGUMENTS!>{}<!>
    (foo2()){}
    (foo2())<!ARGUMENT_TYPE_MISMATCH!>{<!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> }<!>
    foo2()(<!ARGUMENT_TYPE_MISMATCH!>{<!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> }<!>)

    val a = fooT1(1)()
    checkSubtype<Int>(a)

    val b = fooT2<Int>()(1)
    checkSubtype<Int>(b)
    fooT2()(1) // : Any?

    <!UNRESOLVED_REFERENCE!>1<!>()
    <!UNRESOLVED_REFERENCE!>1<!>{}
    <!UNRESOLVED_REFERENCE!>1<!>(){}
}

fun f() :  Int.() -> Unit = {}

fun main1() {
    1.(<!UNRESOLVED_REFERENCE!>fun Int.() = 1<!>)();
    {1}();
    (fun (x : Int) = x)(1)
    1.(<!UNRESOLVED_REFERENCE!>fun Int.(x : Int) = x<!>)(1);
    l@{1}()
    1.((<!UNRESOLVED_REFERENCE!>fun Int.() = 1<!>))()
    1.(<!UNRESOLVED_REFERENCE!>f()<!>)()
    1.<!UNRESOLVED_REFERENCE!>if(true){f()}else{f()}<!>()
    1.<!UNRESOLVED_REFERENCE!>if(true)(fun Int.() {})else{f()}<!>()

    1.<!UNRESOLVED_REFERENCE!>"sdf"<!>()

    1."sdf"
    1.{}
    1.<!INVALID_IF_AS_EXPRESSION!>if<!> (true) {}
}

fun test() {
    {x : Int -> 1}(<!NO_VALUE_FOR_PARAMETER!>)<!>;
    (fun Int.() = 1)(<!NO_VALUE_FOR_PARAMETER!>)<!>
    "sd".(<!UNRESOLVED_REFERENCE!>fun Int.() = 1<!>)()
    val i : Int? = null
    i.(<!UNRESOLVED_REFERENCE!>fun Int.() = 1<!>)();
    <!INAPPLICABLE_CANDIDATE!>{}<!><Int>()
    1<!UNNECESSARY_SAFE_CALL!>?.<!>(<!UNRESOLVED_REFERENCE!>fun Int.() = 1<!>)()
    1.<!UNRESOLVED_REFERENCE!>{}<!>()
}
