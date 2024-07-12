// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-67875

// FILE: MyClass.java
public class MyClass<T> {}

// FILE: MyClassBuilder.java
public class MyClassBuilder<T> {
    public MyClassBuilder<T> makeClass(T... values) {}

    public MyClassBuilder<T> makeClass(Iterable<? extends T> it) {}

    public MyClass<T> build() { return null; }
}

// FILE: main.kt
inline fun <T: Any> myClassOf(
    init: MyClassBuilder<T>.() -> Unit
): MyClass<T> = MyClassBuilder<T>().apply(init).build()

object Foo {
    val myClass = myClassOf {
        makeClass(listOf("a", "b"))
    }

    private val myClassType: MyClass<String> = myClass
}