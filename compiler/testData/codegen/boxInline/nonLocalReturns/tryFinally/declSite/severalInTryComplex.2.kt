package test

class Holder {
    var value: String = ""
}

inline fun doCall(block: ()-> Unit, block2: ()-> Unit, finallyBlock2: ()-> Unit, res: Holder) {
    try {
        try {
            block()
            block2()
        }
        finally {
            finallyBlock2()
        }
    } finally {
        res.value += ", DO_CALL_EXT_FINALLY"
    }
}