// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-68538

fun box(): String {
    val result = buildList {
        var index = 0
        while (index <= 10) {
            if (index > 4) {
                if (index == 5) break
            } else {
                if (index == 3) break
            }
            add(index)
            index++
        }
    }
    return "OK"
}