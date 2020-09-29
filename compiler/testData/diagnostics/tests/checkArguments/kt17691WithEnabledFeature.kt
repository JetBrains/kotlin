// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +UseCorrectExecutionOrderForVarargArguments
// WITH_RUNTIME

fun foo(vararg x: Unit, y: Any) {}

fun main() {
    foo({  }(), y = {  }())
    foo(x = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>arrayOf({  }())<!>, y = {  }())
    foo(x = arrayOf({  }()), y = {  }())
    foo(*arrayOf({  }()), y = {  }())
}

fun foo2(vararg x: Unit, y: Any, z: Any) {}

fun main2() {
    foo2(y = {  }(), x = arrayOf({  }()), z = {  }())
}

fun foo3(vararg x: Unit, y: Any, z: Any = {  }()) {}

fun main3() {
    // no warning, execution order is already right
    foo3(y = {  }(), x = arrayOf({  }()))
}
