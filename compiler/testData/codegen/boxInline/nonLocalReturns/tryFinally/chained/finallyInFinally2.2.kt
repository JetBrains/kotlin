package test

class Holder {
    var value: String = ""
}

inline fun doCall_1(block: ()-> Unit, h: Holder) {
    try {
        doCall(block) {
            h.value += ", OF_FINALLY1"
        }
    } finally {
        h.value += ", DO_CALL_1_FINALLY"
    }
}

inline fun doCall_2(block: ()-> Unit, h: Holder) {
    try {
        doCall(block) {
            try {
                h.value += ", OF_FINALLY1"
            }
            finally {
                h.value += ", OF_FINALLY1_FINALLY"
            }
        }
    } finally {
        h.value += ", DO_CALL_1_FINALLY"
    }
}

inline fun doCall(block: ()-> Unit, finallyBlock1: ()-> Unit) {
    try {
         block()
    } finally {
        finallyBlock1()
    }
}