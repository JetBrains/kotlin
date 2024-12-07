// RUN_PIPELINE_TILL: BACKEND
fun test5() {
    var a: Int
    try {
        <!UNUSED_VALUE!>a =<!> 3
    }
    finally {
        a = 5
    }
    a.hashCode()
}
