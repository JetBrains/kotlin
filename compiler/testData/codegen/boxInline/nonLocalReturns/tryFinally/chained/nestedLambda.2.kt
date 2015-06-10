package test

class Holder {
    var value: String = ""
}

inline fun <R> doCall(block: ()-> R, h: Holder) : R {
    try {
        return block()
    } finally {
        h.value += ", in doCall finally"
    }
}