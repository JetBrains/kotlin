// DONT_TARGET_EXACT_BACKEND: JVM, NATIVE

// RUN_THIRD_PARTY_OPTIMIZER
// WASM_DCE_EXPECTED_OUTPUT_SIZE: wasm 33_968
// WASM_DCE_EXPECTED_OUTPUT_SIZE:  mjs  5_758
// WASM_OPT_EXPECTED_OUTPUT_SIZE:       3_025

// ENABLE_UNUSED_PROPERTY_DCE
// ONLY_IR_DCE
// JS_DROP_REGION_COMMENTS
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR      13_980
// JS_DCE_EXPECTED_OUTPUT_SIZE: JS_IR_ES6  13_726

object Global {
    var a = 5
}

fun write(): Int {
    Global.a += 4
    return Global.a
}

fun read() = Global.a

fun pure() = 10

object WithWrite {
    val x = write()
}

object WithRead {
    val y = read()
}

object WithPure {
    val z = pure()
}

object WithExtraWrite {
    val text = "Hello"
    init {
        write()
    }
}

object WithExtraRead {
    val text = "Hello"
    init {
        read()
    }
}

object WithExtraPure {
    val text = "Hello"
    init {
        pure()
    }
}

object UsedGetFieldInside {
    val anotherText = WithExtraPure.text
}

class CompWithWrite {
    companion object {
        val thing = write()
    }
}

class CompWithRead {
    companion object {
        val thing = read()
    }
}

class CompWithPure {
    companion object {
        val thing = pure()
    }
}

fun box(): String {
    if (WithPure.z != 10) return "Fail WithPure"
    if (WithRead.y != 5) return "Fail WithRead"
    if (WithWrite.x != 9) return "Fail WithWrite"
    if (UsedGetFieldInside.anotherText != "Hello") return "Fail with UsedGetFieldInside"
    if (WithExtraWrite.text != "Hello") return "Fail WithExtraWrite"
    if (WithExtraRead.text != "Hello") return "Fail WithExtraRead"
    if (WithExtraPure.text != "Hello") return "Fail WithExtraPure"
    if (CompWithWrite.thing != 17) return "Fail CompWithWrite"
    if (CompWithRead.thing != 17) return "Fail CompWithRead"
    if (CompWithPure.thing != 10) return "Fail CompWithPure"
    return "OK"
}
