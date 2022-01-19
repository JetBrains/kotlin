fun foo() {}

fun box(): String {
    val x = when ("A") {
        "B" -> foo()
        else -> null
    }

    foo()
    
    return if (x == null) "OK" else "Fail"
}

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 Intrinsics.areEqual
// 1 TABLESWITCH