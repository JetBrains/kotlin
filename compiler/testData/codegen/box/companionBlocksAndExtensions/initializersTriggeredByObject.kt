// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: JVM, JVM_IR, NATIVE

var initOrder = ""

class C {
    companion {
        val blockProp1: String = run { initOrder += "B1 "; "block1" }
    }

    companion object {
        val objectProp: String = run { initOrder += "O "; "object" }
    }

    companion {
        val blockProp2: String = run { initOrder += "B2 "; "block2" }
    }
}

fun box(): String {
    // Access only via companion object - this should trigger
    // initialization of companion block properties as well,
    // since they belong to the same class's static state.
    val op = C.objectProp

    if (op != "object") return "FAIL: objectProp=$op"

    // Companion block properties must have been initialized
    // even though we only accessed the companion object.
    if (C.blockProp1 != "block1") return "FAIL: blockProp1=${C.blockProp1}"
    if (C.blockProp2 != "block2") return "FAIL: blockProp2=${C.blockProp2}"

    // All initializers ran in program order
    if (initOrder != "B1 O B2 ") return "FAIL: initOrder=$initOrder"

    return "OK"
}
