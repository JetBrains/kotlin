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

fun fooT1<T>(t : T) : () -> T {
  return {t}
}

fun fooT2<T>() : (t : T) -> T {
  return {it}
}

fun main(args : Array<String>) {
    args.foo()()
    args.foo1()(<!NO_VALUE_FOR_PARAMETER!>)<!>
    <!UNRESOLVED_REFERENCE!>a<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo1<!>()()
    <!UNRESOLVED_REFERENCE!>a<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo1<!>()(<!UNRESOLVED_REFERENCE!>a<!>)

    args.foo1()(1)
    args.foo1()(<!TYPE_MISMATCH!>"1"<!>)
    <!UNRESOLVED_REFERENCE!>a<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo1<!>()("1")
    <!UNRESOLVED_REFERENCE!>a<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo1<!>()(<!UNRESOLVED_REFERENCE!>a<!>)

    foo2()({})
    foo2()<!TOO_MANY_ARGUMENTS, DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED!>{}<!>
    (foo2()){}
    (foo2()){<!EXPECTED_PARAMETERS_NUMBER_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>x<!> -> }
    foo2()({<!EXPECTED_PARAMETERS_NUMBER_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>x<!> -> })

    val a = fooT1(1)()
    a : Int

    val b = fooT2<Int>()(1)
    b : Int
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>fooT2<!>()(1) // : Any?

    <!FUNCTION_EXPECTED!>1<!>()
    <!FUNCTION_EXPECTED!>1<!>{}
    <!FUNCTION_EXPECTED!>1<!>(){}
}

fun f() :  Int.() -> Unit = {}

fun main1() {
    1.{Int.() -> 1}();
    {1}();
    {(x : Int) -> x}(1)
    1.{Int.(x : Int) -> x}(1);
    @l{1}()
    1.({Int.() -> 1})()
    1.(f())()
    1.if(true){f()}else{f()}()
    1.if(true){Int.() -> <!UNUSED_EXPRESSION!>1<!>}else{f()}()
    1.if(true){Int.() -> 1}else{Int.() -> 1}()

    1.<!FUNCTION_EXPECTED!>"sdf"<!>()

    1.<!ILLEGAL_SELECTOR!>"sdf"<!>
    1.<!ILLEGAL_SELECTOR!>{}<!>
    1.<!ILLEGAL_SELECTOR!>if (true) {}<!>
}

fun test() {
    {(x : Int) -> 1}(<!NO_VALUE_FOR_PARAMETER!>)<!>;
    <!MISSING_RECEIVER!>{Int.() -> 1}<!>()
    <!TYPE_MISMATCH!>"sd"<!>.{Int.() -> 1}()
    val i : Int? = null
    i<!UNSAFE_CALL!>.<!>{Int.() -> 1}();
    {}<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>()
    1<!UNNECESSARY_SAFE_CALL!>?.<!>{Int.() -> 1}()
    1.<!NO_RECEIVER_ADMITTED!>{}<!>()
}