// FILE: A.java
public @interface A {
    Class<?>[] value() default {Integer.class};
    Class<?>[] arg() default {String.class};
}

// FILE: b.kt
A(<!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<Int>()<!>,
<!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<Any>()<!>,
arg = <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>array(javaClass<String>(), javaClass<Double>())<!>)
class MyClass1

A(<!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<Int>()<!>, <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>javaClass<Any>()<!>)
class MyClass2

A(arg = <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>array(javaClass<String>(), javaClass<Double>())<!>)
class MyClass3

A class MyClass4

A(value = *<!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>array(javaClass<Int>(), javaClass<Any>())<!>,
arg = <!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>array(javaClass<String>(), javaClass<Double>())<!>)
class MyClass5

A(value = *<!JAVA_LANG_CLASS_ARGUMENT_IN_ANNOTATION!>array(javaClass<Int>(), javaClass<Any>())<!>)
class MyClass6
