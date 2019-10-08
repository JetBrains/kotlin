// FILE: severalInlineCallsFromOtherFileDex.kt
// EMULATE_DEX: true

package severalInlineCallsFromOtherFileDex

fun main(args: Array<String>) {
    //Breakpoint!
    firstCall()
    secondCall()
}

fun firstCall() {
    inlineFun()
}

fun secondCall() {
    inlineFun()
}

// RESUME: 2
// ADDITIONAL_BREAKPOINT: severalInlineCallsFromOtherFileDex.Other.kt: Breakpoint 1

// FILE: severalInlineCallsFromOtherFileDex.Other.kt
package severalInlineCallsFromOtherFileDex

inline fun inlineFun() {
    var i = 1
    // Breakpoint 1
    i++
    i++
}