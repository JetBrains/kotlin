package some

open class Test
object First: Test() // There's no info for Test in binding context

class Second {
    fun <caret>foo(statement: First) { // Reference to First, to make lazy resolve put it into binding context
    }
}

// For KT-4668
