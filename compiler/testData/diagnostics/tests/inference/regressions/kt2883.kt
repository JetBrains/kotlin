// !WITH_NEW_INFERENCE
//KT-2883 Type inference fails due to non-Unit value returned
package a

public fun doAction(<!UNUSED_PARAMETER!>action<!> : () -> Unit){
}

class Y<TItem>(val itemToString: (TItem) -> String){
}

fun <TItem> bar(<!UNUSED_PARAMETER!>context<!> : Y<TItem>) : TItem{
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun foo(){
    val stringToString : (String) -> String = { it }
    doAction({bar(Y<String>(stringToString))})
}

fun <T> bar(t: T): T = t

fun test() {
    
    doAction { bar(12) }

    val <!UNUSED_VARIABLE!>u<!>: Unit =  <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>bar(11)<!>
}

fun testWithoutInference(col: MutableCollection<Int>) {
    
    doAction { col.add(2) }
    
    val <!UNUSED_VARIABLE!>u<!>: Unit = <!TYPE_MISMATCH!>col.<!NI;TYPE_MISMATCH!>add(2)<!><!>
}