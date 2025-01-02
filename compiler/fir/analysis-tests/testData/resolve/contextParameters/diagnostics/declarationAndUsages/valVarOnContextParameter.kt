// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73671
// LANGUAGE: +ContextParameters

context(<!VAL_OR_VAR_ON_FUN_PARAMETER!>var<!> a: String)
fun test1() {}

context(<!VAL_OR_VAR_ON_FUN_PARAMETER!>val<!> a: String)
fun test2() {}

context(<!VAL_OR_VAR_ON_FUN_PARAMETER!>val<!> a: String)
val property1: String
    get() = a

context(<!VAL_OR_VAR_ON_FUN_PARAMETER!>var<!> a: String)
val property2: String
    get() = a
