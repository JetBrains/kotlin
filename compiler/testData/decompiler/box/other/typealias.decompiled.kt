typealias MyInt = Int
fun box(): String {
    val tmp: Int = 42
    val tmp0_subject: Int = tmp
    when {
        tmp0_subject == 42 -> {
            return "OK"
        }
        else -> {
            return "FAIL"
        }
    }
}
