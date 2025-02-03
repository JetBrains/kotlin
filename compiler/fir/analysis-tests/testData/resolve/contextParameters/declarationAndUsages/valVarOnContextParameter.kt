// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73671
// LANGUAGE: +ContextParameters

// The first context parameter with val/var keyword is parsed as incomplete context list on a property.
context(<!SYNTAX!><!>var a: String<!SYNTAX!>)<!>
fun test1() {}

context(<!SYNTAX!><!>val a: String<!SYNTAX!>)<!>
fun test2() {}

context(<!SYNTAX!><!>val a: String<!SYNTAX!>)<!>
val property1: String
    get() = <!OVERLOAD_RESOLUTION_AMBIGUITY!>a<!>

context(<!SYNTAX!><!>var a: String<!SYNTAX!>)<!>
val property2: String
    get() = <!OVERLOAD_RESOLUTION_AMBIGUITY!>a<!>

context(_: Int, <!VAL_OR_VAR_ON_FUN_PARAMETER!>var<!> a: String)
fun test1() {}

context(_: Int, <!VAL_OR_VAR_ON_FUN_PARAMETER!>val<!> a: String)
fun test2() {}

context(_: Int, <!VAL_OR_VAR_ON_FUN_PARAMETER!>val<!> a: String)
val property1: String
    get() = a

context(_: Int, <!VAL_OR_VAR_ON_FUN_PARAMETER!>var<!> a: String)
val property2: String
    get() = a
