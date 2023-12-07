open class Base(parameter: String)

fun foo(captured: String) {
    class Local : Base {
        val x = captured
        init { println(captured) }
        constructor() : super(captured) {}
        constructor(another: String) : super(another) {}

    }

    Local()
    Local("test")
}

// JVM_TEMPLATES
// 1 final synthetic Ljava/lang/String; \$captured

// JVM_IR_TEMPLATES
// 0 final synthetic Ljava/lang/String; \$captured
