package languageVersion1_1

public fun useJavaMap1_1(): java.util.HashMap<Int, Int> {
    val g = java.util.HashMap<Int, Int>()
    g.values.removeIf { it < 5 }
    return g
}

val use1_0 = languageVersion1_0.useJavaMap1_0().values.removeIf { it < 5 }