// SKIP_TXT

// TESTCASE NUMBER: 1

fun case1() {
    var flag = false
    try {
        throw Exception()
        flag = true
    } catch (e: Exception) {
        assert(!flag)
    }
}
