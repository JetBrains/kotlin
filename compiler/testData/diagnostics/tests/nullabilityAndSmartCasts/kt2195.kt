//KT-2195 error "Only safe calls are allowed ..." but it is function param (val)
package foo

private fun sendCommand<T>(errorCallback: (()->Unit)? = null) {
    if (errorCallback != null) {
        <!DEBUG_INFO_SMARTCAST!>errorCallback<!>()
    }
}