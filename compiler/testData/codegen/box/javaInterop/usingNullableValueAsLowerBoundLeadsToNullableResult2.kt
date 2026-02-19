// TARGET_BACKEND: JVM
// WITH_STDLIB
// ISSUE: KT-81988
// FIR_DUMP

// FILE: J.java
public class J {
    public static <R> R id(R r) { return r; }
}

// FILE: main.kt

fun <O> matNullable(x: (O) -> Unit): O? = null

fun box(): String {
    // O <: String
    // O? <: R!
    //  ==> O & Any..O? <: R
    // fix O := String
    //  => String! <: R
    // fix R := String!
    J.id(matNullable { x: String -> }).toString() // NPE
    return "OK"
}
