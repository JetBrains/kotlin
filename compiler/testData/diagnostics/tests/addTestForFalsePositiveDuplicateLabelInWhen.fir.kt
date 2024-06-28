// ISSUE: KT-26045

class _Class {
    val prop_1 = 1
}

fun case_1(value: Int?, value1: _Class, value2: _Class?) {
    when (value) {
        value1.prop_1, value2?.prop_1 -> {}
        10 -> {}
    }
}

