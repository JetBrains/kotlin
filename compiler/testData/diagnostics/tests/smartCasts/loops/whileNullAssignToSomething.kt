fun main() {
    var result: String? = null
    var i = 0
    while (result == null) {
        if (i == 10) result = "non null"
        else i++
    }
    <!DEBUG_INFO_SMARTCAST!>result<!>.length
}