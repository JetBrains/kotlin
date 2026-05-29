// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: JVM, JVM_IR, NATIVE

var initOrder = ""

class C {
    companion {
        val blockProp1: String = run {
            initOrder += "1"
            "block1"
        }
    }

    companion object {
        val objectProp: String = run {
            initOrder += "2"
            "object"
        }
    }

    companion {
        val blockProp2: String = run {
            initOrder += "3"
            "block2"
        }
    }
}

fun box(): String {
    // Trigger companion initialization by accessing a companion member
    val b1 = C.blockProp1
    val op = C.objectProp
    val b2 = C.blockProp2

    if (b1 != "block1") return "FAIL: blockProp1=$b1"
    if (op != "object") return "FAIL: objectProp=$op"
    if (b2 != "block2") return "FAIL: blockProp2=$b2"

    // §3.2 and §3.2.1: initialization respects program order,
    // including companion objects.
    if (initOrder != "123") return "FAIL: initOrder=$initOrder"

    return "OK"
}
