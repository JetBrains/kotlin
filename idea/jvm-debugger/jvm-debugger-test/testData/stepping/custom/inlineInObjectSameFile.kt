// FILE: inlineInObjectSameFile.kt
package inlineInObjectSameFile

fun main(args: Array<String>) {
    //Breakpoint!
    OtherWithInline.one()
}

object OtherWithInline {
    inline fun one() {
        //Breakpoint!
        println()
    }
}

// RESUME: 1