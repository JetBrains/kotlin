// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: arrayConstructor.kt
fun box(): String {
    val sam = Sam(::IntArray)
    val arr = sam.get(2)
    if (arr.size != 2 || arr[0] != 0 || arr[1] != 0)
        return "Failed"
    return "OK"
}


// FILE: Sam.java
public interface Sam {
    int[] get(int x);
}
