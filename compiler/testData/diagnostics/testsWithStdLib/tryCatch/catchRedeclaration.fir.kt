// !WTIH_NEW_INFERENCE
// SKIP_TXT

class MyException : Exception() {
    val myField = "field"

    fun myFun() {}
}

fun test1() {
    val e = "something"
    try {}
    catch (e: Exception) {
        e.message
        e.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun test2() {
    try {}
    catch (e: Exception) {
        val e = "something"
        e.<!UNRESOLVED_REFERENCE!>message<!>
        e.length
    }
}

fun test3() {
    try {}
    catch (e: MyException) {
        e.myField
    }
}

fun test4() {
    try {}
    catch (e: Exception) {
        val <!REDECLARATION!>a<!> = 42
        val <!REDECLARATION!>a<!> = "foo"
    }
}

fun test5() {
    try {}
    catch (e: Exception) {
        val a: Int = 42
        try {}
        catch (e: MyException) {
            e.myFun()
            val a: String = ""
            a.length
        }
    }
}