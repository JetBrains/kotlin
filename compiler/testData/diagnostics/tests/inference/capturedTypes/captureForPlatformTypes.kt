// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java
public class A {
    public static <T> Class<T> foo(Class<T> clazz) {
        return clazz;
    }

    public static <T> Class<Class<T>> bar(Class<T> clazz) {
        throw new Exception();
    }
}

// FILE: b.kt
fun test1(clazz: Class<out Int>) {
    val foo0: Class<out Int> = A.foo(clazz)
    val foo1 = A.foo(clazz)
    foo1 checkType { _< Class<out Int> >() }
    foo1 checkType { _< Class<out Int?> >() }
}

fun tes2t(clazz: Class<in Int>) {
    val foo0: Class<out Class<in Int>> = A.bar(clazz)
    val foo1 = A.bar(clazz)
    foo1 checkType { _< Class<out Class<in Int>> >() }
    foo1 checkType { _< Class<out Class<in Int?>> >() }
}