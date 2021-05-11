// FIR_COMPARISON
package some

annotation class AnnComplete

class Multiplier {
    @get:[A<caret>]
    var property = 0
}

// ELEMENT: AnnComplete
// FIR_COMPARISON