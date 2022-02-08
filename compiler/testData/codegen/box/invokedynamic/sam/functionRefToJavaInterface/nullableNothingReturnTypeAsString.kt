// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: nullableNothingReturnTypeAsString.kt

fun interface IFoo<T> {
    fun foo(): T
}

var t = "Failed"

fun test(s: String): Nothing? {
    t = s
    return null
}

fun box(): String {
    val q = Sam(::test).get("OK")
    if (q != null)
        return "Failed: $q"
    return t
}


// FILE: Sam.java
public interface Sam {
    String get(String s);
}
