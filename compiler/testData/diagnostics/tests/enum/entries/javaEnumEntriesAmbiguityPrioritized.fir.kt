// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

// FILE: JEnumEntry.java

public enum JEnumEntry {
    entries;
}

// FILE: JEnumStaticField.java

public enum JEnumStaticField {
    ;

    public static final int entries = 0;
}

// FILE: JEnumField.java

public enum JEnumField {
    ;

    public final int entries = 0;
}

// FILE: test.kt

fun test(): String {
    val first = JEnumEntry.<!OVERLOAD_RESOLUTION_AMBIGUITY!>entries<!>
    val second = JEnumStaticField.entries

    val third = JEnumField::entries

    return "$first$second$third"
}
