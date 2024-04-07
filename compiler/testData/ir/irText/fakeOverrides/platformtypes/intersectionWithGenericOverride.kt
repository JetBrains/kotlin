// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// MODULE: separate
// FILE: JavaSeparateModule.java
public interface JavaSeparateModule {
    public int a = 1;
    public int foo();
    public void bar(int o);
}

// MODULE: main
// FILE: Java1.java

public interface Java1<T> {
    public T foo();
    public void bar(T o);
}

// FILE: Java2.java

public interface Java2<T> {
    public T foo();
    public void bar(T o);
}

// FILE: Java3.java

public class Java3<T> {
    public T a;
    public T foo(){return a;};
    public void bar(T o){};
}

// FILE: 1.kt

interface A<R> : Java1<R>, Java2<R> //Kotlin ← Java1, Java2

class B<R> : Java1<R>, Java3<R>()   //Kotlin ← Java1, Java2

interface C<R> : Java1<R>, KotlinInterface<R>   //Kotlin ← Java, Kotlin2

interface D<R>: Java1<R>, Java2<R>, KotlinInterface<R>  //Kotlin ← Java1, Java2, Kotlin2

class E<R>: Java1<R>, Java2<R>, Java3<R>()  //Kotlin ← Java1, Java2, Java3

interface F<R>: Java1<R>, KotlinInterface<R>, KotlinInterface2<R>   //Kotlin ← Java, Kotlin1, Kotlin2

interface KotlinInterface<T> {
    var a: T
    fun foo(): T
    fun bar(o: T)
}

interface KotlinInterface2<T> {
    var a: T
    fun foo(): T
    fun bar(o: T)
}

fun test(a: A<Int>, b: B<String>, c: C<Int>, d: D<String>, e: E<Int>, f: F<String>) {
    val k: Int = a.foo()
    val k2: Unit = a.bar(2)
    val k3: String = b.a
    b.a = "1"
    val k4: String = b.foo()
    val k5: Unit = b.bar(null)
    val k6: Unit = b.bar("")
    val k7: Int = c.a
    c.a = 1
    val k8: Int = c.foo()
    val k9: Unit = c.bar(1)
    val k10: Unit = c.bar(null)
    val k11: String = d.a
    d.a = ""
    val k12: String = d.foo()
    val k13: Unit = d.bar("")
    val k14: Unit = d.bar(null)
    val k15: Int = e.a
    e.a = 1
    val k16: Int = e.foo()
    val k17: Unit = e.bar(1)
    val k18: Unit = e.bar(null)
    val k19: String = f.a
    f.a = ""
    val k20: String = f.foo()
    val k21: Unit = f.bar("")
    val k22: Unit = f.bar(null)
}