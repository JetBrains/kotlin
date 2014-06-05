import test.*

class Holder(var value: Int)

fun test1(holder: Holder, doNonLocal: Boolean) {
    holder.value = -1;

    val localResult = doCall {
        if (doNonLocal) {
            holder.value = 1000
            return
        }
        10
    }

    holder.value = localResult
}


fun box(): String {
    val h = Holder(-1)

    test1(h, false)
    if (h.value != 10) return "test1: ${h.value}"

    test1(h, true)
    if (h.value != 1000) return "test2: ${h.value}"

    return "OK"
}
