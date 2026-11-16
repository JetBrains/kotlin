// TARGET_BACKEND: JVM
// FILE: p/Chain.java
package p;

public class Chain {
    public static final int C0 = 1;
    public static final int C1 = C0 + C0;
    public static final int C2 = C1 + C1;
    public static final int C3 = C2 + C2;
    public static final int C4 = C3 + C3;
    public static final int C5 = C4 + C4;
    public static final int C6 = C5 + C5;
    public static final int C7 = C6 + C6;
    public static final int C8 = C7 + C7;
    public static final int C9 = C8 + C8;
    public static final int C10 = C9 + C9;
    public static final int C11 = C10 + C10;
    public static final int C12 = C11 + C11;
    public static final int C13 = C12 + C12;
    public static final int C14 = C13 + C13;
    public static final int C15 = C14 + C14;
    public static final int C16 = C15 + C15;
    public static final int C17 = C16 + C16;
    public static final int C18 = C17 + C17;
    public static final int C19 = C18 + C18;
    public static final int C20 = C19 + C19;
    public static final int C21 = C20 + C20;
    public static final int C22 = C21 + C21;
    public static final int C23 = C22 + C22;
    public static final int C24 = C23 + C23;
    public static final int C25 = C24 + C24;
    public static final int C26 = C25 + C25;
    public static final int C27 = C26 + C26;
    public static final int C28 = C27 + C27;
    public static final int C29 = C28 + C28;
    public static final int C30 = C29 + C29;
}

// FILE: test.kt
const val K = p.Chain.C30

fun box(): String = if (K == 1_073_741_824) "OK" else "FAIL: $K"
