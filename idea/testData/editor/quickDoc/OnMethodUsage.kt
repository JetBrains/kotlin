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

// INFO: <b>internal</b> <b>fun</b> testMethod(a: jet.Int, b: jet.String): jet.Unit <i>defined in</i> root package<br/><p>Some documentation<br/><br/><b>@param</b> - <i>a</i> - Some int<br/><b>@param</b> - <i>b</i>: String<br/></p>