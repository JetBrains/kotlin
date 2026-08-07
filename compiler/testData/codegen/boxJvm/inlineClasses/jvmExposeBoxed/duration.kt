// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@JvmExposeBoxed("createDuration")
fun makeDuration(): Duration = 2.seconds

@JvmExposeBoxed
class Timeout @JvmExposeBoxed constructor(val duration: Duration)

// FILE: Main.java
public class Main {
    public kotlin.time.Duration test() {
        return new Timeout(ICKt.createDuration()).getDuration();
    }
}

// FILE: Box.kt
import kotlin.time.Duration.Companion.seconds

fun box(): String {
    val duration = Main().test()
    if (duration != 2.seconds) return "FAIL: $duration"
    return "OK"
}
