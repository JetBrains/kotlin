// RUN_PIPELINE_TILL: FRONTEND
fun test1() {
    <!NEW_INFERENCE_ERROR!>try {
        { <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>toDouble<!> ->
        }
    } catch (e: Exception) {

    }<!>
}

fun test2() {
    <!NEW_INFERENCE_ERROR!>try {

    } catch (e: Exception) {
        { <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>toDouble<!> ->
        }
    }<!>
}

fun box(): String {
    test1()
    test2()
    return "OK"
}
