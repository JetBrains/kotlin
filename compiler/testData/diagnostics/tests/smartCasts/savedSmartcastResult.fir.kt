// ISSUE: KT-25747
fun test1() {
    val nullableString: String? = ""
    val savedSmartCastResult = nullableString != null
    if (savedSmartCastResult) {
        nullableString.length
    }
}

fun test2() {
    var nullableAny: Any? = ""
    val savedSmartCastResult = nullableAny is String
    nullableAny = 10
    if (savedSmartCastResult) {
        nullableAny.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

class A {
    val a: String? = ""
}
fun test3(a: A){
    val savedSmartCastResult = a.a != null
    if(savedSmartCastResult) {
        a.a.length
    }
}

fun test4() {
    val nullableAny: Any? = ""
    val savedSmartCastResult = (nullableAny!= null && nullableAny is String?)
    if(savedSmartCastResult) {
        nullableAny.length
    }
}