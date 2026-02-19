// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// TARGET_BACKEND: JVM_IR
//todo: check with lightweightlambdas and without it

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class TopLevelValueClass(val s: String)

@get:JvmExposeBoxed
val lambda : () ->  TopLevelValueClass = {TopLevelValueClass("OK")}

// FILE: Test.java

public class Test {
    public String test() {
        return ICKt.getLambda().invoke().getS();
    }
}

// FILE: Box.kt
fun box(): String {
    return Test().test()
}
