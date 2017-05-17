package test

// extra parameter is to make sure generic signature is not erased
fun doNothing(array: kotlin.IntArray, ignore: java.util.List<String>) = array
