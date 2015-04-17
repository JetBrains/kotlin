// FILE: A.java
public @interface A {
    Class<?> x() default Integer.class;
    int y();
}

// FILE: b.kt
[A(x = <!TYPE_MISMATCH(kotlin.reflect.KClass<*>; java.lang.Class<kotlin.Any>)!>javaClass<Any>()<!>, y = <!TYPE_MISMATCH!>""<!>)] fun test1() {}
