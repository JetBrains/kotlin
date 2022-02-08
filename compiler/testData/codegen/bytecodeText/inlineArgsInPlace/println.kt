
fun test() {
    println("Hello, world!")
}

// JVM_IR_TEMPLATES:
// 0 ALOAD
// 0 ASTORE
// 1 SWAP
//  ^ here temporary variable elimination kicks in (instead of inplace argument inlining), producing a SWAP instruction
