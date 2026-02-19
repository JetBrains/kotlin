open class Base(parameter: String)

fun foo(captured: String) {
    object : Base(captured) {
        val x = captured
        init { println(captured) }
    }
}

// 0 final synthetic Ljava/lang/String; \$captured
