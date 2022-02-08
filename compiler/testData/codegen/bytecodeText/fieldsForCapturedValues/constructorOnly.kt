open class Base(parameter: String)

fun foo(captured: String) {
    object : Base(captured) {
        val x = captured
        init { println(captured) }
    }
}

// JVM_TEMPLATES
// 1 final synthetic Ljava/lang/String; \$captured

// JVM_IR_TEMPLATES
// 0 final synthetic Ljava/lang/String; \$captured
