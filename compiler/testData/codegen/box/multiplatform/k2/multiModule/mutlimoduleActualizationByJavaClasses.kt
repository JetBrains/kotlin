// TARGET_BACKEND: JVM_IR
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-72725
// WITH_STDLIB
// FULL_JDK

// MODULE: commonLib
// FILE: commonLib.kt
expect abstract class A

// MODULE: platformLib()()(commonLib)
// FILE: MyA.java
public abstract class MyA {
    String o = "O";
    public String k = "K";
}

// FILE: lib-platform.kt
actual typealias A = MyA


// MODULE: common(platformLib)
// FILE: common.kt
expect open class B : A

// MODULE: jvm(platformLib)()(common)
// FILE: MyB.java
public class MyB extends MyA {}

// FILE: jvm.kt
actual typealias B = MyB

fun box(): String {
    val b = B()
    return b.o + b.k
}
