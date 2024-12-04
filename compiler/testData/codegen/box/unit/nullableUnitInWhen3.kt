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
// 0 Intrinsics.areEqual
// 1 TABLESWITCH
