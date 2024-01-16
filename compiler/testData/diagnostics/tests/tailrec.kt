// FIR_IDENTICAL
// ISSUE: KT-63529

private tailrec fun Context.findActivityOrNull(): Activity? {
    return mBase<!UNNECESSARY_SAFE_CALL!>?.<!>findActivityOrNull()
}
abstract class Context
open class Activity
var mBase: Context = TODO()
