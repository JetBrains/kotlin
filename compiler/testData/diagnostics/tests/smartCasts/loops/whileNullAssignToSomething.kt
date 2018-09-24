fun main(args: Array<String>) {
    var result: String? = null
    var i = 0
    while (result == null) {
        if (i == 10) result = "non null"
        else i++
    }
    result<!UNSAFE_CALL!>.<!>length
}