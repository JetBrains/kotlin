// FIR_IDENTICAL
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP
// ISSUE: KT-56623

// FILE: JavaEnum.java
public enum JavaEnum {
    public static String entries =  "entries";
}

// FILE: I.java
interface I {
    String entries = "entries";
}

// FILE: JavaEnumI.java
public enum JavaEnumI implements I {}

// FILE: Main.kt
fun main() {
    println(JavaEnum.entries)
    println(JavaEnumI.entries)
}
