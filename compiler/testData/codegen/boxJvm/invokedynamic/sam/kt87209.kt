// TARGET_BACKEND: JVM
// FULL_JDK
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// 2 java/lang/invoke/LambdaMetafactory
// 0 private final static box\$run

import java.util.function.Consumer

fun consume(consumer: Consumer<Runnable>) {
    consumer.accept(Runnable { })
}

fun box(): String {
    consume(Runnable::run)
    return "OK"
}
