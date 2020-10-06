// IGNORE_BACKEND_FIR: JVM_IR
fun main(args: Array<String>) {
    var i = 10

    // -- 4 of these fit in iinc instructions
    i += 1
    i += 127
    i += 128
    i += -1
    i += -128
    i += -129

    // -- 4 of these fit in iinc instructions
    i -= 1
    i -= 128
    i -= 129
    i -= -1
    i -= -127
    i -= -128

    // -- 4 of these fit in iinc instructions
    i = i + 1
    i = i + 127
    i = i + 128
    i = i + -1
    i = i + -128
    i = i + -129

    // -- 4 of these fit in iinc instructions
    i = i - 1
    i = i - 128
    i = i - 129
    i = i - -1
    i = i - -127
    i = i - -128
}

// JVM_IR_TEMPLATES
// 16 IINC

// JVM_TEMPLATES
// 0 IINC