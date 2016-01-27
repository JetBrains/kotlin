// !DIAGNOSTICS: -UNUSED_PARAMETER
class My {
    constructor(arg: Int?) {
        run {
            var x = arg
            if (x == null) return@run
            <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
        }   
    }
}