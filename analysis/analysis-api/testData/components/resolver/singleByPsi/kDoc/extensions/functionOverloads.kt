package test

class Base {
    fun function() {}
}

fun Any.function() {}

fun Base.function(i: Int) {}

/**
 * [test.<caret_1>function]
 * [<caret_2>function]
 * 
 * [Base.<caret_3>function]
 * [test.Base.<caret_4>function]
 */
fun usage() {}