// WITH_STDLIB
// SKIP_TXT
// !LANGUAGE: +InlineLateinit
// FIR_IDENTICAL

@JvmInline
value class IC1(val x: Int)

@JvmInline
value class IC2(val x: IC1)

@JvmInline
value class IC3(val x: String)

@JvmInline
value class IC4(val x: String?)

@JvmInline
value class IC5(val x: IC4)

@JvmInline
value class IC6<T>(val x: T)

@JvmInline
value class IC7<T : Any>(val x: T)

@JvmInline
value class IC8(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>IC9<!>)

@JvmInline
value class IC9(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>IC8<!>)

@JvmInline
value class IC10(val x: IC6<String>)

@JvmInline
value class IC11(val x : IC4?)

<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var a: IC1
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var b: IC2
lateinit var c: IC3
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var d: IC4
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e: IC6<String>
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var f: IC6<*>
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var g: IC5
lateinit var h: IC7<Double>
lateinit var i: IC7<*>
lateinit var j: IC8
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var k : IC10
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var o : IC3?
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var m : UInt
<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var n : IC11

class B {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var a: IC1
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var b: IC2
    lateinit var c: IC3
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var d: IC4
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e: IC6<String>
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var f: IC6<*>
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var g: IC5
    lateinit var h: IC7<Double>
    lateinit var i: IC7<*>
    lateinit var j: IC8
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var k : IC10
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var o : IC3?
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var m : UInt
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var n : IC11
}

fun foo() {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var a: IC1
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var b: IC2
    lateinit var c: IC3
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var d: IC4
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var e: IC6<String>
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var f: IC6<*>
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var g: IC5
    lateinit var h: IC7<Double>
    lateinit var i: IC7<*>
    lateinit var j: IC8
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var k : IC10
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var o : IC3?
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var m : UInt
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var n : IC11
}