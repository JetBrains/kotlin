// !LANGUAGE: +SoundSmartcastFromLoopConditionForLoopAssignedVariables

fun foo() {
    var x: String? = "123"
    while (x!!.length < 42) {
        x = null
        break

    }
    // TODO: this testdata fixates undesired behavior (it should be an unsafe call)
    <!DEBUG_INFO_SMARTCAST!>x<!>.length // 'x' is unsoundly smartcasted here
}

fun bar() {
    var x: List<Int>? = ArrayList<Int>(1)
    for (i in x!!) {
        x = null
        break

    }
    // TODO: this testdata fixates undesired behavior (it should be an unsafe call)
    <!DEBUG_INFO_SMARTCAST!>x<!>.size // 'x' is unsoundly smartcasted here
}