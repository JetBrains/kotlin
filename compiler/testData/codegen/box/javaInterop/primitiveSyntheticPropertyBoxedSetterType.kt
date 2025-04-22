// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-76805
// FILE: To.java
class To {
    boolean mTo;
    boolean getTo() { return mTo; }
    void setTo(Boolean to) { mTo = to != null && to; }

    static Boolean someValue = null;
}

// FILE: box.kt
fun box(): String {
    To().to = To.someValue;
    return "OK"
}