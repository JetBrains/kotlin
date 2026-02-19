// FILE: pkg/JavaClass.java
package pkg;

public class JavaClass<T1> {
    public static <T2> void staticMember() {}
}

// FILE: main.kt
package test

import pkg.JavaClass

class MyClass

fun constructor() {
    JavaClass<<caret_constructor_MyClass>MyClass>()
    JavaClass<test.<caret_constructor_MyClass_fqn>MyClass>()

    JavaClass<<caret_constructor_List>List<MyClass>>()
    JavaClass<<caret_constructor_MutableList>MutableList<MyClass>>()

    JavaClass<List<<caret_constructor_List_MyClass>MyClass>>()
}

fun staticMember() {
    JavaClass.staticMember<<caret_staticMember_MyClass>MyClass>()
    JavaClass.staticMember<test.<caret_staticMember_MyClass_fqn>MyClass>()

    JavaClass.staticMember<<caret_staticMember_List>List<MyClass>>()
    JavaClass.staticMember<<caret_staticMember_MutableList>MutableList<MyClass>>()

    JavaClass.staticMember<List<<caret_staticMember_List_MyClass>MyClass>>()
}
