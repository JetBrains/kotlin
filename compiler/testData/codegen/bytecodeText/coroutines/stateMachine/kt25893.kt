class Handler(val func: suspend (Any) -> Unit)

inline fun createHandler(crossinline handler: suspend (Any) -> Unit): Handler {
    return Handler({ handler.invoke(it) })
}

fun main(args: Array<String>) {
    createHandler({
        if (it !is String) {}
    })
}

// JVM_TEMPLATES
// 2 TABLESWITCH

// JVM_IR_TEMPLATES
// lambda inside main is tail-call
// 1 TABLESWITCH