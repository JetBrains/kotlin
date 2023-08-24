// FIR_IDENTICAL
// !LANGUAGE: +EnumEntries +PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP
// ISSUE: KT-56623

// FILE: JavaEnum.java
public enum JavaEnum {
    public static String entries =  "entries";
}

// FILE: JavaEnum01.java

interface I01 {
    String entries = "entries";
}

public enum JavaEnum01 implements I01 {
}

// FILE: Main.kt
fun main() {
    println(JavaEnum.entries)
    println(JavaEnum01.entries)
}
