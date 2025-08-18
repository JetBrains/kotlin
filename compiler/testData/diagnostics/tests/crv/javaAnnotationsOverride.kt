// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// FILE: com/google/errorprone/annotations/CheckReturnValue.java
package com.google.errorprone.annotations;

public @interface CheckReturnValue {}

// FILE: com/google/errorprone/annotations/CanIgnoreReturnValue.java
package com.google.errorprone.annotations;

public @interface CanIgnoreReturnValue {}

// FILE: app/JavaList.java

package app;

import com.google.errorprone.annotations.*;

@CheckReturnValue
public class JavaList extends java.util.AbstractList<String> {
    @Override
    public String get(int index) {
        return "";
    }

    @Override
    @CanIgnoreReturnValue
    public boolean add(String s) {
        return super.add(s);
    }

    @Override
    @CanIgnoreReturnValue
    public String set(int index, String element) {
        return "";
    }

    @Override
    public int size() {
        return 0;
    }

    public int notAListMember() {
        return 0;
    }
}

// FILE: app/UnannotatedList.java
package app;

public class UnannotatedList extends java.util.AbstractList<String> {
    @Override
    public String get(int index) {
        return "";
    }

    @Override
    public boolean add(String s) {
        return super.add(s);
    }

    @Override
    public String set(int index, String element) {
        return "";
    }

    @Override
    public int size() {
        return 0;
    }

    public int notAListMember() {
        return 0;
    }
}

// FILE: Lists.kt

package app;

class List1: JavaList() {
    override fun get(index: Int) = ""
    override fun notAListMember(): Int = 42
}

@MustUseReturnValue
class List2: JavaList() {
    override fun get(index: Int): String = ""
    // Not sure why K1 reports parameter name change, probably some quirk in handling java list as supertype
    override fun add(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>s<!>: String?): Boolean = true

    @IgnorableReturnValue
    override fun set(index: Int, element: String?): String {
        return "super.set(index, element)"
    }
}

class List3: UnannotatedList() {
    override fun get(index: Int) = ""
    override fun notAListMember(): Int = 42
}

@MustUseReturnValue
class List4: UnannotatedList() {
    override fun get(index: Int): String = ""
    // Not sure why K1 reports parameter name change, probably some quirk in handling java list as supertype
    override fun add(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>s<!>: String?): Boolean = true

    @IgnorableReturnValue
    override fun set(index: Int, element: String?): String {
        return "super.set(index, element)"
    }
}

fun test1(l: List1) {
    l.get(0)
    l.notAListMember()
    l.size
    l.set(0, "")
    l.add("")
}

fun test2(l: List2) {
    l.get(0)
    l.notAListMember()
    l.size
    l.set(0, "")
    l.add("")
}

fun test3(l: List3) {
    // Due to enhancements, UnnanotatedList.get/size get @MURV from kotlin.collections.List.get/size
    l.get(0)
    l.notAListMember()
    l.size
    l.set(0, "")
    l.add("")
}

fun test4(l: List4) {
    l.get(0)
    l.notAListMember()
    l.size
    l.set(0, "")
    l.add("")
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, integerLiteral, javaFunction, javaProperty,
javaType, nullableType, operator, override, stringLiteral */
