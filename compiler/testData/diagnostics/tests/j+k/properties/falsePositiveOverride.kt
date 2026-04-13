// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// ISSUE: KT-85213

// FILE: MyList.java
public abstract class MyList<E> extends java.util.AbstractCollection<E> implements java.util.List<E> {
    public int length() { return 0; }

    @Override
    public int size() { return length(); }
}

// FILE: main.kt

fun foo(list: MyList<Any>) {
    list.length()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, objectDeclaration */
