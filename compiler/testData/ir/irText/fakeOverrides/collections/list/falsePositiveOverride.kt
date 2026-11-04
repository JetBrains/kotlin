// TARGET_BACKEND: JVM
// SKIP_KT_DUMP
// FULL_JDK
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib reads Java declarations from compiled classes, not from sources.
// FILE: MyList.java
public abstract class MyList<E> extends java.util.AbstractCollection<E> implements java.util.List<E> {
    public int length() { return 0; }

    @Override
    public int size() { return length(); }
}

// FILE: main.kt

abstract class SubList<F> : MyList<F>()

fun foo(list1: MyList<Any>, list2: SubList<Any>) {
    list1.length()
    list2.length()
}
