// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// FILE: Java1.java

public interface Java1<T> {
    public void foo(T t);
    public T bar();
}

// FILE: Java2.java

public interface Java2 {
    public void foo(Object t);

    public Object bar();
}

// FILE: 1.kt
interface A : KotlinInterface<Nothing?>, Java1<Nothing?>

interface B: KotlinInterface2, Java2

interface KotlinInterface<T> {
    var a: T
    fun foo(o: T)
    fun bar(): T
}

interface KotlinInterface2 {
    var a: Nothing?
    fun foo(o: Nothing?)
    fun bar(): Nothing?
}

fun test(a: A, b: B) {
    val k: Nothing? = a.a
    val k2: Unit = a.foo(k)
    val k3: Nothing? = a.bar()
    a.foo(null)

    val k4: Nothing? = b.a
    val k5: Unit = b.foo(k)
    val k6: Nothing? = b.bar()
    b.foo(null)
}