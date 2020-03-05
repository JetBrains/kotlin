// !WITH_NEW_INFERENCE
// JAVAC_EXPECTED_FILE
// See also KT-10735
fun test() {
    var a: Int?
    try {
        a = 3
    }
    catch (e: Exception) {
        return
    }
    a.hashCode() // a is never null here
}
class A: Exception()
class B: Exception()
fun test2() {
    var a: Int?
    try {
        a = 4
    }
    catch (e: A) {
        return
    }
    catch (e: B) {
        return
    }
    a.hashCode() // a is never null here
}
fun test3() {
    var a: Int? = null
    try {
        a = 5
    }
    catch (e: A) {
        // do nothing
    }
    catch (e: B) {
        return
    }
    a.hashCode() // a is nullable here
}
fun test4() {
    var a: Int? = null
    try {
        // do nothing
    }
    catch (e: A) {
        return
    }
    catch (e: B) {
        return
    }
    a.<!INAPPLICABLE_CANDIDATE!>hashCode<!>() // a is nullable here
}
fun test5() {
    var a: Int?// = null
    try {
        a = 3
    }
    catch (e: Exception) {
        return
    }
    finally {
        a = 5
    }
    a.hashCode() // a is never null here
}
fun test6() {
    var a: Int?// = null
    try {
        a = 3
    }
    catch (e: Exception) {
        return
    }
    finally {
        a = null
    }
    a.<!INAPPLICABLE_CANDIDATE!>hashCode<!>() // a is null here
}
