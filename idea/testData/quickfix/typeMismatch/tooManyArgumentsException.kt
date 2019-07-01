// "???" "false"
// ERROR: Type mismatch: inferred type is Array<out Int> but Array<out String> was expected
// ACTION: Change parameter 't' type of function 'join' to 'Array<out Int>'
// ACTION: Create function 'join'
// ACTION: Flip ','
// ACTION: Introduce local variable
// ACTION: Convert to also
// ACTION: Convert to apply
// ACTION: Put arguments on separate lines

//this test checks that there is no ArrayIndexOutOfBoundsException when there are more arguments than parameters
fun <T> array1(vararg a : T) = a

fun main(args : Array<String>) {
    val b = array1(1, 1)
    join(1, "4", *b<caret>, "3")
}

fun join(x : Int, vararg t : String) : String = "$x$t"
