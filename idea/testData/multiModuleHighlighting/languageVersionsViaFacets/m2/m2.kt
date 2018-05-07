package languageVersion1_0

public fun useJavaMap1_0(): java.util.HashMap<Int, Int> {
    val g = java.util.HashMap<Int, Int>()
    g.values.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: removeIf">removeIf</error> { <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: it">it</error> <error descr="[DEBUG] Resolved to error element"><</error> 5 }
    return g
}

val use1_1 = languageVersion1_1.useJavaMap1_1().values.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: removeIf">removeIf</error> { <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: it">it</error> <error descr="[DEBUG] Resolved to error element"><</error> 5 }

