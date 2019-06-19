fun f() {
    for (i in 0..5 step 2) {
    }

    // JVM non-IR: `step 1` suppresses optimized code generation for 'for-in-downTo'
    // JVM IR: No getProgressionLastElement() call required for `step 1`, equivalent to `5 downTo 1`
    for (i in 5 downTo 1 step 1) {
    }
}

// JVM non-IR does NOT specifically handle "step" progressions. The stepped progressions in the above code are constructed and their
// first/last/step properties are retrieved.
// JVM IR has an optimized handler for "step" progressions and elides the construction of the stepped progressions.
//
// Expected lowered form of `for (i in 0..5 step 2)`:
//
//   // Standard form of loop over progression
//   var inductionVar = 0
//   val last = getProgressionLastElement(0, 5, 2)
//   val step = 2
//   if (inductionVar <= last) {
//     // Loop is not empty
//     do {
//       val i = inductionVar
//       inductionVar += step
//       // Loop body
//     } while (i != last)
//   }
//
// Expected lowered form of `for (i in 5 downTo 1 step 1)`:
//
//   // Standard form of loop over progression
//   var inductionVar = 5
//   val last = 1
//   val step = -1
//   if (last <= inductionVar) {   // Optimized out in bytecode
//     // Loop is not empty
//     do {
//       val i = inductionVar
//       inductionVar += step
//       // Loop body
//     } while (last <= inductionVar)
//   }

// 0 iterator

// JVM_TEMPLATES
// 2 getFirst
// 2 getLast
// 2 getStep

// JVM_IR_TEMPLATES
// 0 getStart
// 0 getEnd
// 0 getFirst
// 0 getLast
// 0 getStep
// 1 IF_ICMPGT
// 1 IF_ICMPNE
// 1 IF_ICMPLE
// 3 IF
// 1 INVOKESTATIC kotlin/internal/ProgressionUtilKt.getProgressionLastElement \(III\)I