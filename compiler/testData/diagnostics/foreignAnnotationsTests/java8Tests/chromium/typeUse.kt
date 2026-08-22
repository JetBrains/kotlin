// NULLABILITY_ANNOTATIONS: @org.chromium.build.annotations:strict
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: TypeUse.java

import java.util.List;

import org.chromium.build.annotations.*;

@NullMarked
public class TypeUse {
    public List<@Nullable String> listOfNullable() { return null; }

    public @Nullable List<String> nullableList() { return null; }

    public String[] arrayOfNotNull() { return null; }

    public @Nullable String[] arrayOfNullable() { return null; }

    public String @Nullable [] nullableArray() { return null; }

    public void consumeListOfNullable(List<@Nullable String> arg) {}
}

// FILE: Box.java

import java.util.List;

import org.chromium.build.annotations.*;

@NullMarked
public class Box<T extends @Nullable Object> {
    public T get() { return null; }

    public List<@NonNull T> nonNullElements() { return null; }
}

// FILE: main.kt

fun typeUse(t: TypeUse) {
    t.listOfNullable()[0]<!UNSAFE_CALL!>.<!>length
    t.listOfNullable()[0]?.length
    t.listOfNullable().size

    t.nullableList()<!UNSAFE_CALL!>.<!>size
    t.nullableList()?.get(0)?.length

    t.arrayOfNotNull()[0].length
    t.arrayOfNullable()[0]<!UNSAFE_CALL!>.<!>length
    t.nullableArray()<!UNSAFE_CALL!>.<!>size
    t.nullableArray()?.get(0)?.length

    t.consumeListOfNullable(listOf(null))
    t.consumeListOfNullable(listOf(""))
}

fun box(b1: Box<String>, b2: Box<String?>) {
    b1.get().length
    b2.get()<!UNSAFE_CALL!>.<!>length
    b2.get()?.length

    b1.nonNullElements()[0].length
    b2.nonNullElements()[0].length
}
