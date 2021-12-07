fun test(a: Any?) {
    if (a is String) {
        (@Denotable("kotlin.String") a).length
        if (a is Int) {
            (@Denotable("kotlin.Int") a).inc()
        }
        if (a is String) {
            (@Denotable("kotlin.String") a).length
        }
    }
    if (a != null) {
        (@Denotable("kotlin.Any") a).hashCode()
    }
    if (a == null) {
        (@Denotable("kotlin.Any?") a).isNothing()
    }
    if (a is String || a is Int) {
        (@Denotable("kotlin.Any?") a).length
        (@Denotable("kotlin.Any?") a).inc()
    }
    @Nondenotable("(kotlin.Comparable<*> & java.io.Serializable)") if (true) {
        ""
    } else {
        1
    }
}

fun Nothing?.isNothing() {}
