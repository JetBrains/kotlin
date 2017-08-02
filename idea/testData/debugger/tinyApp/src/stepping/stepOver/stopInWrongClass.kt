package stopInWrongClass

class AA {
    fun test() {
        // Should be on the same line with Breakpoint 1
        foo()
    }

    fun other() {
        //Breakpoint!
        foo()
    }
}

fun main(args: Array<String>) {
    // Shouldn't stop inside test()
    AA().test()
    AA().other()
}

// ADDITIONAL_BREAKPOINT: stopInWrongClass.Other.kt: Breakpoint 1