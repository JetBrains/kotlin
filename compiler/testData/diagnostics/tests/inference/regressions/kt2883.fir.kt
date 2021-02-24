// !WITH_NEW_INFERENCE
//KT-2883 Type inference fails due to non-Unit value returned
package a

public fun doAction(action : () -> Unit){
}

class Y<TItem>(val itemToString: (TItem) -> String){
}

fun <TItem> bar(context : Y<TItem>) : TItem{
}

fun foo(){
    val stringToString : (String) -> String = { it }
    doAction({bar(Y<String>(stringToString))})
}

fun <T> bar(t: T): T = t

fun test() {
    
    doAction { bar(12) }

    val u: Unit =  bar(11)
}

fun testWithoutInference(col: MutableCollection<Int>) {
    
    doAction { col.add(2) }
    
    val u: Unit = col.add(2)
}
