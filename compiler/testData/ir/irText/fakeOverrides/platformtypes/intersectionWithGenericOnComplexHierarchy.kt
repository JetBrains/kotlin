// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public interface Java1<T>  {
    T foo();
    void bar(T o);
}

// FILE: Java2.java
public interface Java2<T> extends A<T> { }

// FILE: Java3.java
public interface Java3<T> {
    T foo();
    void bar(T o);
}

// FILE: Java4.java
public interface Java4<T> extends Java1<T> { }

// FILE: 1.kt

interface A<T> {
    var a: T;
    fun foo(): T;
    fun bar(o: T);
}

abstract class B<T> : Java1<T>, Java2<T> //Kotlin ← Java1, Java2 ← Kotlin2

class C<T>(override var a: T) : Java1<T>, Java2<T> {    //Kotlin ← Java1, Java2 ← Kotlin2 with explicit override
    override fun bar(o: T) {}
    override fun foo(): T {
        return null!!
    }
}

abstract class D<T> : Kotlin<T>, Java2<T>    // Kotlin ← Java, Kotlin2 ← Kotlin3

class E<T>(override var a: T) : Kotlin<T>, Java2<T> {   // Kotlin ← Java, Kotlin2 ← Kotlin3 with explicit override
    override fun bar(o: T) {}

    override fun foo(): T {
        return null!!
    }
}

abstract class F<T> : Kotlin2<T>, Java3<T>   //Kotlin ← Java, Kotlin2 ← Java2

class G<T> : Kotlin2<T>, Java3<T> { //Kotlin ← Java, Kotlin2 ← Java2 with explicit override
    override fun bar(o: T) {}

    override fun foo(): T {
        return null!!
    }
}

abstract class H<T> : Java4<T>, Java3<T>     //Kotlin ← Java1, Java2 ← Java3

class I<T> : Java4<T>, Java3<T> {  //Kotlin ← Java1, Java2 ← Java3 with explicit override
    override fun bar(o: T) {}

    override fun foo(): T {
        return null!!
    }
}

interface Kotlin<T> {
    fun foo(): T
    fun bar(o: T)
}

interface Kotlin2<T> : Java1<T>

fun test(b: B<Int>, c: C<Any>, d: D<Any?>, e: E<Int>, f: F<Any>,
         g: G<String>, h: H<Any?>, i: I<String?>) {
    val k: Int = b.a
    b.a = 1
    val k2: Unit = b.bar(1)
    val k3: Unit = b.bar(null)
    val k4: Int = b.foo()
    val k5: Any = c.a
    c.a = ""
    val k6: Unit = c.bar(1)
    val k7: Unit = c.bar("")
    val k8: Any = c.foo()
    val k9: Any? = d.a
    d.a = null
    val k10: Unit = d.bar(1)
    val k11: Unit = d.bar("")
    val k12: Unit = d.bar(null)
    val k13: Any? = d.foo()
    val k14: Int = e.a
    e.a = 1
    val k15: Unit = e.bar(1)
    val k16: Int = e.foo()
    val k17: Unit = f.bar(1)
    val k18: Unit = f.bar("")
    val k19: Unit = f.bar(null)
    val k20: Any = f.foo()
    val k21: Unit = g.bar("")
    val k22: String = g.foo()
    val k23: Unit = h.bar(1)
    val k24: Unit = h.bar("")
    val k25: Unit = h.bar(null)
    val k26: Any? = h.foo()
    val k27: Unit = i.bar("")
    val k28: Unit = i.bar(null)
    val k29: String? = i.foo()
}