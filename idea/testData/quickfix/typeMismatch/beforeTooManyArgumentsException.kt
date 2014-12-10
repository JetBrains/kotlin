// "???" "false"
//ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Array&lt;out kotlin.String&gt;</td></tr><tr><td>Found:</td><td>kotlin.Array&lt;out kotlin.Int&gt;</td></tr></table></html>

//this test checks that there is no ArrayIndexOutOfBoundsException when there are more arguments than parameters
fun <T> array1(vararg a : T) = a

fun main(args : Array<String>) {
    val b = array1(1, 1)
    join(1, "4", *b, "3")
}

fun join(x : Int, vararg t : String) : String = "$x$t"
