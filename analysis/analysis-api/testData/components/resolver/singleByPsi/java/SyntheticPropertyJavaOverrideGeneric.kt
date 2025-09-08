// FILE: JavaClass.java
public class JavaClass extends KotlinClass<String> {
    @Override
    public Foo<String> getFoo() { return null; }
    @Override
    public void setFoo(Foo<String> foo) {}
}

// FILE: main.kt
abstract class KotlinClass<T> {
    abstract var foo: Foo<T>
}

interface Foo<T> {
}

fun JavaClass.test(javaClass: JavaClass, foo: Foo<String>) {
    print(javaClass.<caret_1>foo)
    javaClass.<caret_2>foo = foo
}

fun <T> bar(kotlinClass: KotlinClass<T>) {
    if (kotlinClass is JavaClass) {
        print(kotlinClass.<caret_3>foo)
    }
}