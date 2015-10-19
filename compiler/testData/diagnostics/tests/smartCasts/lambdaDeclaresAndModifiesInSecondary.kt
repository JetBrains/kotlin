// !DIAGNOSTICS: -UNUSED_PARAMETER
fun run(f: () -> Unit) = 0

class My {
    constructor(arg: Int?) {
        run {
            var x = arg
            if (x == null) return@run
            <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
        }   
    }
}