// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt

package builders

inline fun call(crossinline init: () -> Unit) {
    return init()
}

// FILE: 2.kt

import builders.*


fun test(): String {
    var res = "Fail"

    call {
        val lambda ={
            res = "OK"
        }; lambda()
    }

    return res
}


fun box(): String {
    return test()
}
