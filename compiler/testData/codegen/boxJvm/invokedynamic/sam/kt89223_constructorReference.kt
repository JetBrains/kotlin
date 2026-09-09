// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// FULL_JDK
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// 2 java/lang/invoke/LambdaMetafactory

import java.util.concurrent.ThreadFactory

fun consume(factory: ThreadFactory) {
    factory.newThread(Runnable { }).run()
}

fun box(): String {
    consume(ThreadFactory(::Thread))
    return "OK"
}
