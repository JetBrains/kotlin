// WITH_STDLIB
fun find(): String {
    for (x in sequence { yield(1); yield(2); yield(3) }) {
        if (x == 2) return "OK"
    }
    return "Fail"
}
fun box(): String = find()
