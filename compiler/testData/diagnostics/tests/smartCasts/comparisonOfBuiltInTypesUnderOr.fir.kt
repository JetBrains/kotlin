// ISSUE: KT-1982
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

fun test1(x: Any?) {
    if (x is Any || <!SENSELESS_COMPARISON!>x == null<!>) {
        var k: Any? = x
    } else {
        var k: Any = x
        var k2: Nothing = x
    }
}

fun test2(x: Any?) {
    if (x is Any || <!SENSELESS_COMPARISON!>x != null<!>) {
        var k: Any = x
    } else {
        var k: Any? = x
    }
}

fun test3(x: Any?) {
    if (<!USELESS_IS_CHECK!>x is Any?<!> || <!SENSELESS_COMPARISON!>x != null<!>) {
        var k: Any? = x
    } else {
        var k: Nothing = x
        var k2: Int = x
    }
}

fun test4(x: Any?) {
    if (x !is Any || <!SENSELESS_COMPARISON!>x != null<!>) {
        var k: Any? = x
    } else {
        var k: Nothing = x
        var k2: Any? = x
    }
}

fun test5(x: Any?) {
    if (x is String || x == null){
        var k: String? = x
    }
    else {
        var k: Any = x
    }
}

fun test6(x: Any?) {
    if (x !is String || <!SENSELESS_COMPARISON!>x == null<!>){
        var k: Any? = x
    }
    else {
        var k: String = x
    }
}

fun test7(x: Any?) {
    if (x is String || x != null){
        var k: Any = x
    }
    else {
        var k: Any? = x
    }
}

fun test8(x: Any?) {
    if (x !is String || <!SENSELESS_COMPARISON!>x != null<!>){
        var k: Any? = x
    }
    else {
        var k: String? = x
    }
}

fun test9(x: Any?) {
    if (x is String?){
        var k: String? = x
    }
    else {
        var k: Any = x
    }
}

fun test10(x: Any?) {
    if (x !is String?){
        var k: Any = x
    }
    else {
        var k: String? = x
    }
}

fun test11(x: Any?) {
    if (x is List<*> || x == null){
        var k : List<*>? = x
    }
    else {
        var k: Any = x
    }
}

fun test12(x: Any?) {
    if (x is List<*> || x != null){
        var k2: Any = x
    }
    else {
        var k: Any? = x
    }
}

fun test13(list: List<String>?) {
    if (list is ArrayList || list == null) {
        var k: ArrayList<String>? = list
    }
}

fun test14(list: List<String>?) {
    if (list is ArrayList || list != null) {
        var k: ArrayList<String>? = <!INITIALIZER_TYPE_MISMATCH!>list<!>
    }
}