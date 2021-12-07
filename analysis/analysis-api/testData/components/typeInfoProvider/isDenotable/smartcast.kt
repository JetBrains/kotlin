fun test(a: Any?) {
    if (a is String) {
        (@Denotable("kotlin.String") a).length
        if (a is Int) {
            (@Nondenotable("(kotlin.String&kotlin.Int)") a).inc()
        }
        if (a is String) {
            (@Denotable("kotlin.String") a).length
        }
    }
    if (a != null) {
        (@Denotable("kotlin.Any") a).hashCode()
    }
    if (a == null) {
        (@Denotable("kotlin.Nothing?") a).isNothing()
    }
    if (a is String || a is Int) {
        (@Nondenotable("(kotlin.Comparable<(kotlin.String&kotlin.Int)>&java.io.Serializable)") a).length
        (@Nondenotable("(kotlin.Comparable<(kotlin.String&kotlin.Int)>&java.io.Serializable)") a).inc()
    }
    @Nondenotable("(kotlin.Comparable<*>&java.io.Serializable)") if (true) {
        ""
    } else {
        1
    }
}

fun Nothing?.isNothing() {}
