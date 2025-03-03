// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// TARGET_BACKEND: JVM_IR
//todo: check with lightweightlambdas and without it
// JVM_EXPOSE_BOXED

// FILE: IC.kt
@JvmInline
value class TopLevelValueClass(val s: String)

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
