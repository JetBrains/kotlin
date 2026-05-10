// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.4
// ^^^ kotlin.ClassCastException: class kotlin.String cannot be cast to class kotlin.Int
//     this issue was introduced in 2.3.20 in scope of KT-83036 and fixed in 2.4.20 by cd21b5bb23ad
fun box(): String {
    var x: Any = 42

    try {
        try {
            x = "OK"
            throw Error()
        } catch (e: Exception) {
        }
    } catch (e: Throwable) {
        return x.toString()
    }

    return "fail"
}
