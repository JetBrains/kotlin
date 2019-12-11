// !WITH_NEW_INFERENCE
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

    raw.charSequences = arrayOf<String>()
    raw.charSequences = arrayOf<Double>()

    raw.maps = arrayOf<Map<Int, Int>>()
    raw.maps = arrayOf<MutableMap<Int, Int>>()
    raw.maps = arrayOf<List<String>>()

    raw.arraysOfLists = arrayOf<Array<List<*>>>()
    raw.arraysOfLists = arrayOf<List<String>>()
    raw.arraysOfLists = arrayOf<Array<Array<String>>>()

    raw.arraysOfAny = arrayOf<Array<Array<String>>>()

    raw.erasedLists = arrayOf<List<String>>()
}
