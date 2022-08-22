fun f() = IntArray(1) { run { return@IntArray 1 } }

// JVM_TEMPLATES
// 0 IntRef

// On JVM_IR, the return is an assignment to a captured var followed by
// a non-local `break` from a `do ... while (false)`. The var should be optimized, but InnerClasses attribute will be added anyway.
// JVM_IR_TEMPLATES
// 2 IntRef
// 1 INNERCLASS kotlin.jvm.internal.Ref\$IntRef kotlin.jvm.internal.Ref IntRef