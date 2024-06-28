class Test {
    val booleanValue: Boolean = false
    val booleanNullableValue: Boolean = false
    val callableVal: (() -> Boolean)? = null
    val a: A? = null
    fun doAction() {}
}

class TestTypeParameter<T>{
    val data: T? = null
    fun doAction() { }
}

class A {
    val booleanValue: Boolean? = null
    fun doAction() {}
}

fun test1(test:Test?){
    val doAction = test?.booleanValue ?: false
    if(doAction){
        test<!UNSAFE_CALL!>.<!>doAction()
    }
}

fun test2(test:Test?){
    var doAction = test?.booleanValue ?: false
    if(doAction){
        test<!UNSAFE_CALL!>.<!>doAction()
    }
}

fun test3(test:Test?){
    val doAction = test?.booleanNullableValue ?: false
    if(doAction){
        test<!UNSAFE_CALL!>.<!>doAction()
    }
}

fun test4(test:Test?){
    val doAction = test?.callableVal?.invoke() ?: false
    if(doAction){
        test<!UNSAFE_CALL!>.<!>doAction()
    }
}

fun test5(test:Test?){
    val doAction = test?.a?.booleanValue ?: false
    if(doAction){
        test<!UNSAFE_CALL!>.<!>doAction()
        test<!UNSAFE_CALL!>.<!>a<!UNSAFE_CALL!>.<!>doAction()
        val b: Boolean = <!TYPE_MISMATCH!>test<!UNSAFE_CALL!>.<!>a<!UNSAFE_CALL!>.<!>booleanValue<!>
    }
}

fun test6(test:Test?){
    val temp = test
    val doAction = temp?.booleanValue ?: false
    if(doAction){
        test<!UNSAFE_CALL!>.<!>doAction()
    }
}

fun test7(test:Test?){
    val doAction = test?.booleanValue ?: false
    val temp = doAction
    if(temp) {
        test<!UNSAFE_CALL!>.<!>doAction()
    }
}

fun test8(test:Test?) {
    val doAction = test?.booleanValue ?: throw Exception()
    if (doAction) {
        <!DEBUG_INFO_SMARTCAST!>test<!>.doAction()
    }
    <!DEBUG_INFO_SMARTCAST!>test<!>.doAction()
}

fun test9(test:Test?) {
    val doAction = test?.booleanValue ?: return
    if (doAction) {
        <!DEBUG_INFO_SMARTCAST!>test<!>.doAction()
    }
    <!DEBUG_INFO_SMARTCAST!>test<!>.doAction()
}

fun test10(test:Test?) {
    val doAction = test!!.booleanNullableValue <!USELESS_ELVIS!>?: false<!>
    if (doAction) {
        <!DEBUG_INFO_SMARTCAST!>test<!>.doAction()
    }
    <!DEBUG_INFO_SMARTCAST!>test<!>.doAction()
}

fun test11(test: TestTypeParameter<Boolean>?){
    val doAction = test?.data ?: false
    if (doAction) {
        test<!UNSAFE_CALL!>.<!>doAction()
    }
}

fun test12(test: TestTypeParameter<out Boolean>?){
    val doAction = test?.data ?: false
    if (doAction) {
        test<!UNSAFE_CALL!>.<!>doAction()
    }
}

fun test13(test: TestTypeParameter<*>?){
    val doAction = test?.data ?: false
    if (doAction != false) {
        test<!UNSAFE_CALL!>.<!>doAction()
    }
}

