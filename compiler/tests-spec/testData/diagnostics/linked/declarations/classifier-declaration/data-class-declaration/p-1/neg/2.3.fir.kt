// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
fun case1(){
    val x = TODO()
    data class A1(val x : Any, y: Any)
}


// TESTCASE NUMBER: 2
fun case2(){
    val x = TODO()
    data class A1(val x : Any, vararg y: Any)
}

// TESTCASE NUMBER: 3
fun case3(){
    val x = TODO()
    data class A1(val x : Any, y: Any = 1)
}


// TESTCASE NUMBER: 4
fun case4(){
    val x = TODO()
    data class A1(val x : Any, vararg y: Any = TODO())
}
