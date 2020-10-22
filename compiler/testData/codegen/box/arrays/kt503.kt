// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun iarr(vararg a : Int) = a
fun <T> array(vararg a : T) = a

fun box() : String {
    val tests = array<IntArray>(
        iarr(6, 5, 4, 3, 2, 1),
        iarr(1, 2),
        iarr(1, 2, 3),
        iarr(1, 2, 3, 4),
        iarr(1)
    )

    var n = 0

    try {
        var i = 0
        while (true) {
            if (thirdElementIsThree(tests[i++]))
              n++
        }
    }
    catch (e : ArrayIndexOutOfBoundsException) {
        // No more tests to process
    }
    return if(n == 2) "OK" else "fail"
}

fun thirdElementIsThree(a : IntArray) =
    a.size >= 3 && a[2] == 3
