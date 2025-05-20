// IGNORE_STABILITY_K2: candidates

// FILE: main.kt
class KotlinClass : JavaClass<Any>() {
    override fun getFoo(): Foo<Any> {
        TODO("Not yet implemented")
    }

    override fun setFoo(foo: Foo<Any>) {
    }
}

fun KotlinClass.test(kotlinClass: KotlinClass, foo: Foo<Any>) {
    print(kotlinClass.<caret_1>foo)
    kotlinClass.<caret_2>foo = foo
}

fun <T> bar(javaClass: JavaClass<T>) {
    if (javaClass is KotlinClass) {
        print(javaClass.<caret_3>foo)
    }
}

// FILE: JavaClass.java
public abstract class JavaClass<T> {
    public abstract Foo<T> getFoo();
    public abstract void setFoo(Foo<T> foo);
}

// FILE: Foo.java
public interface Foo<T> {
}
