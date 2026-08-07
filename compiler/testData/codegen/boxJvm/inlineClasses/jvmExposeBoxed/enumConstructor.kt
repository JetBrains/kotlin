// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// An enum constructor is always private, so there is nothing to expose there; the class-level annotation must
// still reach the property accessor. The explicit-annotation counterpart of 'directive/kt85955.kt'.
@JvmExposeBoxed
enum class Entry(val id: Id = Id("2")) {
    ONE(Id("1")),
    TWO()
}

// FILE: Main.java
public class Main {
    public String explicitArgument() {
        return Entry.ONE.getId().getValue();
    }

    public String defaultArgument() {
        return Entry.TWO.getId().getValue();
    }
}

// FILE: Box.kt
fun box(): String {
    var res = Main().explicitArgument()
    if (res != "1") return "FAIL 1: $res"
    res = Main().defaultArgument()
    if (res != "2") return "FAIL 2: $res"
    return "OK"
}
