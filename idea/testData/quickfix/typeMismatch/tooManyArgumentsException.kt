// "???" "false"
//ERROR: Type mismatch: inferred type is kotlin.Array<out kotlin.Int> but kotlin.Array<out kotlin.String> was expected

//this test checks that there is no ArrayIndexOutOfBoundsException when there are more arguments than parameters
fun <T> array1(vararg a : T) = a

fun main(args : Array<String>) {
    val b = array1(1, 1)
    join(1, "4", *b, "3")
}

fun join(x : Int, vararg t : String) : String = "$x$t"
