// !DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST
// !LANGUAGE: +NewInference
// SKIP_TXT
// Issue: KT-20849

fun <T>test_1(x: T.() -> T): T = null as T
fun <T>test_2(x: (T) -> T): T = null as T
fun <T>test_3(x: T.(T) -> T): T = null as T
fun <T>test_4(x: T.(T) -> List<T>): T = null as T
fun <T>test_5(): List<T> = 10 <!CAST_NEVER_SUCCEEDS!>as<!> List<T>
fun <K, V>test_6(): Map<K, V> = 10 <!CAST_NEVER_SUCCEEDS!>as<!> Map<K, V>

fun case_1() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test_5<!>()
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test_6<!>()
    val <!UNUSED_VARIABLE!>x<!> = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test_6<!>()
}

fun case_2() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test_1<!> { null!! }
}

fun case_3() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test_2<!> { null!! }
}

fun case_4() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test_3<!> { null!! }
}

fun case_5() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test_4<!> { null!! }
}

fun case_6() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test_1<!> { throw <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>Exception<!>() }
}

fun case_7() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test_2<!> { throw Exception() }
}

fun case_8() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test_3<!> { throw <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>Exception<!>() }
}

fun case_9() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>test_4<!> { throw <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>Exception<!>() }
}
