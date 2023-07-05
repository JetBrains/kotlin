// DIAGNOSTICS: -DEBUG_INFO_MISSING_UNRESOLVED
// ISSUE: KT-55933

fun swapVararg(vararg namedVararg: String, named: Int){}
fun swapVararg(named: Int, vararg namedVararg: String){}

val x = swapVararg(named = 42, namedVararg = arrayOf("1", "2"),)

fun swap(string: String, int: Int){}
fun swap(int: Int, string: String){}

val y = swap(int = 42, string = "2")
