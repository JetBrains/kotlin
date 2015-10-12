package kt939

//KT-939 CommonSupertypes erases scopes associated to types

fun compare(o1 : String?, o2 : String?) : Int {
    val l1 = o1?.length ?: 0
    val l2 = o2?.length ?: 0
    return l1 - l2 // '-' is unresolved, because the type of l1 is Int with an empty member scope
}

//KT-1117 Unresolved reference to multiply sign

fun test() {
    (System.getProperty("path.separator")?.length ?: 4) * 55 + 5

    val x = System.getProperty("path.separator")?.length ?: 4
    x * 55 + 5
}
