// TARGET_BACKEND: JVM_IR
fun box(): String {
    for (i in 2 until Int.MIN_VALUE step 2) {
    }

    return "OK"
}

// For "until" progressions in JVM IR, there is a check that the range is not empty: upper bound != MIN_VALUE.
// When the upper bound == const MIN_VALUE, the backend can eliminate the entire loop as dead code.
// Calls to getProgressionLastElement() are still present because these happen before the eliminated loop.
//
// Expected lowered form of loop (before bytecode optimizations):
//
//   // Standard form of loop over progression
//   var inductionVar = 2
//   val last = getProgressionLastElement(1, Int.MIN_VALUE - 1, 2)   // `(Int.MIN_VALUE - 1)` underflows to 2147483647
//   val step = 2
//   if (false && inductionVar <= last) {   // `false` comes from constant folding of `Int.MIN_VALUE != Int.MIN_VALUE`
//     // Loop is not empty
//     do {
//       val i = inductionVar
//       inductionVar += step
//       // Loop body
//     } while (i != last)
//   }

// 0 iterator
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 INVOKESTATIC kotlin/internal/ProgressionUtilKt.getProgressionLastElement
// 0 NEW java/lang/IllegalArgumentException
// 0 ATHROW
// 0 IF