// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java

import java.util.*;

public class A<T extends CharSequence, E> {
    T[] charSequences;
    Map<String, T>[] maps;
    List<Double>[][] arraysOfLists;
    E[][] arraysOfAny;

    List<Double[]>[] erasedLists;
}

// FILE: Test.java

class Test {
    static class RawADerived extends A {}
    static A rawAField = null;
}

// FILE: main.kt

fun <T> arrayOf(): Array<T> = null!!

fun main() {
    val raw = Test.rawAField

    raw.charSequences = <!TYPE_MISMATCH, TYPE_MISMATCH!>arrayOf<String>()<!>
    raw.charSequences = <!TYPE_MISMATCH, TYPE_MISMATCH!>arrayOf<Double>()<!>

    raw.maps = <!TYPE_MISMATCH, TYPE_MISMATCH!>arrayOf<Map<Int, Int>>()<!>
    raw.maps = <!TYPE_MISMATCH, TYPE_MISMATCH!>arrayOf<MutableMap<Int, Int>>()<!>
    raw.maps = <!TYPE_MISMATCH, TYPE_MISMATCH!>arrayOf<List<String>>()<!>

    raw.arraysOfLists = <!TYPE_MISMATCH, TYPE_MISMATCH!>arrayOf<Array<List<*>>>()<!>
    raw.arraysOfLists = <!TYPE_MISMATCH, TYPE_MISMATCH!>arrayOf<List<String>>()<!>
    raw.arraysOfLists = <!TYPE_MISMATCH, TYPE_MISMATCH!>arrayOf<Array<Array<String>>>()<!>

    raw.arraysOfAny = <!TYPE_MISMATCH, TYPE_MISMATCH!>arrayOf<Array<Array<String>>>()<!>

    raw.erasedLists = <!TYPE_MISMATCH, TYPE_MISMATCH!>arrayOf<List<String>>()<!>
}
