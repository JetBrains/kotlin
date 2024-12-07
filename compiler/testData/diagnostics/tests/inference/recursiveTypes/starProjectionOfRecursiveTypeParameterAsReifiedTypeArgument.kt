// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-56901

fun test() {
    val childrenBox = Box(arrayOf<Child?>(null))
    val parentsBox = childrenBox as Box<*> // Box<*> == Box<out Parent<out Parent<...>>>
    parentsBox.refresh() // pollute an Array<Child?> property with an Array<out Parent<out Parent<...>>?> object
    childrenBox.storage[0] // K/JVM: CCE (class [LParent; cannot be cast to class [LChild;)
}

interface Parent<PT>

class Box<BT: Parent<BT>>(var storage: Array<BT?>)

inline fun <reified RT: Parent<RT>> Box<RT>.refresh() {
    storage = arrayOf<RT?>(null)
}

interface Child: Parent<Child>
