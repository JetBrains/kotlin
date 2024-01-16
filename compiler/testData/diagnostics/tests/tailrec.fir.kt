// ISSUE: KT-63529

private <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun Context.findActivityOrNull(): Activity? {
    return mBase<!UNNECESSARY_SAFE_CALL!>?.<!><!NON_TAIL_RECURSIVE_CALL!>findActivityOrNull<!>()
}
abstract class Context
open class Activity
var mBase: Context = TODO()