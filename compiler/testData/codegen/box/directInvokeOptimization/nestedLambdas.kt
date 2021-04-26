// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 Function(^.)*.invoke

fun box() =
    { k: String ->
        { o: String ->
            var ok = o
            ok += k
            ok
        }("O")
    }("K")
