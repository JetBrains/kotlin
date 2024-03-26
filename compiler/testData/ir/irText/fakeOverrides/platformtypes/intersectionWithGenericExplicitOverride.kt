// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

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
    public T foo(){ return a;};
    public void bar(T o){};
}

// FILE: 1.kt

class A<R> : Java1<R>, Java2<R> {    //Kotlin ← Java1, Java2
    override fun foo(): R {
        return null!!
    }

    override fun bar(o: R) {}
}

class B<R> : Java1<R>, Java3<R>() { //Kotlin ← Java1, Java2
    override fun foo(): R {
        return null!!
    }

    override fun bar(o: R) {}
}

class C<R>(override var a: R) : Java1<R>, KotlinInterface<R> {   //Kotlin ← Java, Kotlin2
    override fun foo(): R {
        return null!!
    }

    override fun bar(o: R) {}
}

class D<R>(override var a: R) : Java1<R>, Java2<R>, KotlinInterface<R> { //Kotlin ← Java1, Java2, Kotlin2
    override fun foo(): R {
        return null!!
    }

    override fun bar(o: R) {}
}

class E<R> : Java1<R>, Java2<R>, Java3<R>() {   //Kotlin ← Java1, Java2, Java3
    override fun foo(): R {
        return super.foo()
    }

    override fun bar(o: R) {
        super.bar(o)
    }
}

class F<R> : Java1<R>, KotlinInterface<R>, KotlinInterface2<R> {  //Kotlin ← Java, Kotlin1, Kotlin2
    override var a: R
        get() = null!!
        set(value) {}

    override fun foo(): R {
        return null!!
    }

    override fun bar(o: R) {}
}

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

fun test(a: A<Int>, b: B<Int>, c: C<String>, d: D<Int>, e: E<Int>, f: F<Int>) {
    val k: Int = a.foo()
    val k2: Unit = a.bar(1)
    val k3: Int = b.foo()
    val k4: Unit = b.bar(1)
    val k5: String = c.foo()
    val k6: Unit = c.bar("")
    val k7: String = c.a
    c.a = ""
    val k8: Int = d.foo()
    val k9: Unit = d.bar(1)
    val k10: Int = d.a
    d.a = 1
    val k11: Int = e.foo()
    val k12: Unit = e.bar(1)
    val k13: Int = e.a
    e.a = 1
    val k14: Int = f.foo()
    val k15: Unit = f.bar(1)
    val k16: Int = f.a
    f.a = 1
}