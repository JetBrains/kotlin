// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-77563

class Test() {
    init {
        var num: Int? = 3
        val numNotNull = num!!
        for (x in 1..2) {
            val num2 = num
            num<!UNSAFE_CALL!>.<!>inc()
            num = null
        }
    }
}
