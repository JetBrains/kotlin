// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public interface Java1  {
    Object a = null;
    Object foo();
    void bar(Object o);
}

// FILE: Java2.java
public interface Java2 extends A { }

// FILE: Java3.java
public interface Java3 {
    int a = 1;
    int foo();
    void bar(int o);
}

// FILE: Java4.java
public interface Java4 extends Java1 { }

// FILE: 1.kt

interface A {
    var a: Int;
    fun foo(): Int;
    fun bar(o: Int);
}

abstract class B : Java1, Java2 //Kotlin ← Java1, Java2 ← Kotlin2

class C(override var a: Int) : Java1, Java2 { //Kotlin ← Java1, Java2 ← Kotlin2 with explicit override
    override fun bar(o: Int) { }

    override fun foo(): Int {
        return 1
    }

    override fun bar(o: Any?) { }
}

abstract class D : Java1, Java2 {   //Kotlin ← Java1, Java2 ← Kotlin2 with explicit override
    override fun bar(o: Int) { }

    override fun foo(): Int {
        return 1
    }
}

abstract class E : Java1, Java2 {   //Kotlin ← Java1, Java2 ← Kotlin2 with explicit override
    override fun bar(o: Any) { }
}

abstract class F : Kotlin, Java2    // Kotlin ← Java, Kotlin2 ← Kotlin3

abstract class G(override var a: Int) : Kotlin, Java2 { // Kotlin ← Java, Kotlin2 ← Kotlin3 with explicit override
    override fun foo(): Int {
        return 1
    }
}

class H(override var a: Int) : Kotlin, Java2 { // Kotlin ← Java, Kotlin2 ← Kotlin3 with explicit override
    override fun bar(o: Int) { }

    override fun foo(): Int {
        return 1
    }

    override fun bar(o: Any) { }
}

abstract class I : Kotlin2, Java3   //Kotlin ← Java, Kotlin2 ← Java2

class J : Kotlin2, Java3 {  //Kotlin ← Java, Kotlin2 ← Java2 with explicit override
    override fun bar(o: Int) { }

    override fun bar(o: Any?) { }

    override fun foo(): Int {
        return 1
    }

}

abstract class L : Kotlin2, Java3 { //Kotlin ← Java, Kotlin2 ← Java2 with explicit override
    override fun bar(o: Any?) { }
}

abstract class M : Java4, Java3     //Kotlin ← Java1, Java2 ← Java3

class N : Java4, Java3 {    //Kotlin ← Java1, Java2 ← Java3 with explicit override
    override fun bar(o: Int) { }

    override fun bar(o: Any?) { }

    override fun foo(): Int {
        return 1
    }
}

abstract class O : Java4, Java3 {   //Kotlin ← Java1, Java2 ← Java3 with explicit override
    override fun bar(o: Int) { }

    override fun foo(): Int {
        return 1
    }
}

interface Kotlin {
    fun foo(): Any;
    fun bar(o: Any);
}

interface Kotlin2 : Java1

fun test(b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j:J, l: L,
         m: M, n: N, o: O) {
    val k: Int = b.a
    b.a = 1
    val k2: Unit = b.bar(1)
    val k3: Unit = b.bar("")
    val k4: Unit = b.bar(null)
    val k5: Int = b.foo()
    val k6: Int = c.a
    c.a = 1
    val k7: Unit = c.bar(1)
    val k8: Unit = c.bar("")
    val k9: Unit = c.bar(null)
    val k10: Int = c.foo()
    val k11: Int = d.a
    d.a = 1
    val k12: Unit = d.bar(1)
    val k13: Unit = d.bar("")
    val k14: Unit = d.bar(null)
    val k15: Int = d.foo()
    val k16: Int = e.a
    e.a = 1
    val k17: Unit = e.bar(1)
    val k18: Unit = e.bar("")
    val k19: Int = e.foo()
    val k20: Int = f.a
    f.a = 1
    val k21: Unit = f.bar(1)
    val k22: Unit = f.bar("")
    val k23: Int = f.foo()
    val k24: Int = g.a
    g.a = 1
    val k25: Unit = g.bar(1)
    val k26: Unit = g.bar("")
    val k27: Int = f.foo()
    val k28: Int = h.a
    h.a = 1
    val k29: Unit = h.bar(1)
    val k30: Unit = h.bar("")
    val k31: Int = h.foo()
    val k32: Unit = i.bar(1)
    val k33: Unit = i.bar("")
    val k34: Unit = i.bar(null)
    val k35: Int = i.foo()
    val k36: Unit = j.bar(1)
    val k37: Unit = j.bar("")
    val k38: Unit = j.bar(null)
    val k39: Int = j.foo()
    val k40: Unit = l.bar(1)
    val k41: Unit = l.bar("")
    val k42: Unit = l.bar(null)
    val k43: Int = l.foo()
    val k44: Unit = m.bar(1)
    val k45: Unit = m.bar("")
    val k46: Unit = m.bar(null)
    val k47: Int = m.foo()
    val k48: Unit = n.bar(1)
    val k49: Unit = n.bar("")
    val k50: Unit = n.bar(null)
    val k51: Int = n.foo()
    val k52: Unit = o.bar(1)
    val k53: Unit = o.bar("")
    val k54: Unit = o.bar(null)
    val k55: Int = o.foo()
}