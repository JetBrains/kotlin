/**
 Some documentation

 @param a - Some int
 * @param b: String
 */
fun testMethod(a: Int, b: String) {

}

fun test() {
    <caret>testMethod(1, "value")
}

// INFO: <b>internal</b> <b>fun</b> testMethod(a: Int, b: String): Unit<br/><p>Some documentation<br/><br/><b>@param</b> - <i>a</i> - Some int<br/><b>@param</b> - <i>b</i>: String<br/></p>