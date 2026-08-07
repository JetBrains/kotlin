// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Money(val cents: Int) {
    operator fun plus(other: Money): Money = Money(cents + other.cents)

    infix fun combineWith(other: Money): Money = Money(cents * other.cents)
}

// FILE: Main.java
public class Main {
    public int operator() {
        return new Money(1).plus(new Money(2)).getCents();
    }

    public int infix() {
        return new Money(3).combineWith(new Money(4)).getCents();
    }
}

// FILE: Box.kt
fun box(): String {
    var res = Main().operator()
    if (res != 3) return "FAIL 1: $res"
    res = Main().infix()
    if (res != 12) return "FAIL 2: $res"
    return "OK"
}
