// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: charArrayOf.kt
fun box(): String {
    val sam = Sam(::charArrayOf)
    val arr = sam.get(charArrayOf('O', 'K'))
    return "${arr[0]}${arr[1]}"
}

// FILE: Sam.java
public interface Sam {
    char[] get(char[] s);
}
