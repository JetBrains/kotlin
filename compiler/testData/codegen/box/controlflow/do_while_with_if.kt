// KT-73130

fun box(): String {
    var result = "OK"
    val someCondition = true
    val value = 10
    val test = 23

    do {
        if (someCondition) {
            break
        }

        result = "Failed: should not reach this point"
    } while (value != test)

    return result
}
