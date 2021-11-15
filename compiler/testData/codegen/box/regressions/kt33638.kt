// WITH_STDLIB

class Holder(val list: List<String>?)

fun box(): String {
    val holder1 = Holder(emptyList()) // No problem
    val holder2 = Holder(if(true) emptyList<String>() else null) // No problem
    val holder3 = Holder(if(true) emptyList() else mutableListOf()) // No problem
    val holder4 = Holder(if(true) emptyList() else null) // Compile error
    return "OK"
}
