// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +EnumEntries
// WITH_STDLIB
// FIR_DUMP

// FILE: pkg/JEnumStaticField.java

package pkg;

public enum JEnumStaticField {
    ;

    public static final String entries = "ENTRIES";
    public static final String somethingElse = "...";
}

// FILE: test.kt

import pkg.JEnumStaticField.entries
import pkg.JEnumStaticField.somethingElse

val entries = 0
val somethingElse = 1

fun test() {
    entries.length
    somethingElse.length
}
