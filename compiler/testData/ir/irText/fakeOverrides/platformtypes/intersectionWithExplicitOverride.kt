// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// MODULE: separate
// FILE: SeparateModuleJava1.java
public interface SeparateModuleJava1 {
    public Object a = 0;
    public Object foo();
    public void bar(Object o);
}

// FILE: SeparateModuleJava2.java
public interface SeparateModuleJava2 {
    public int a = 1;
    public int foo();
    public void bar(int o);
}

// MODULE: main
// FILE: Java1.java

public interface Java1 {
    public Object a = 0;
    public Object foo();
    public void bar(Object o);
}

// FILE: Java2.java

public interface Java2 {
    public int a = 1;
    public int foo();
    public void bar(int o);
}

// FILE: Java3.java

public interface Java3 {
    public Number a = 1;
    public Number foo();
    public void bar(Number o);
}

// FILE: 1.kt

class A : Java1, Java2 {     //Kotlin ← Java1, Java2
    override fun foo(): Int {
        return 1
    }

    override fun bar(o: Any?) {}
    override fun bar(o: Int) {}
}

abstract class B : Java1, Java2 {    //Kotlin ← Java1, Java2
    override fun bar(o: Int) {}
}

class C : SeparateModuleJava1, SeparateModuleJava2 {     //Kotlin ← Java1, Java2 (separate module)
    override fun bar(o: Int) {}

    override fun bar(o: Any?) {}

    override fun foo(): Int {
        return 1
    }
}

class D : Java1, SeparateModuleJava2 {  //Kotlin ← Java1, Java2 (separate module)
    override fun foo(): Int {
        return 1
    }

    override fun bar(o: Any?) {}

    override fun bar(o: Int) {}
}

class E(override var a: Int) : Java1, KotlinInterface { //Kotlin ← Java, Kotlin2
    override fun bar(o: Int) {}

    override fun bar(o: Any?) {}

    override fun foo(): Int {
        return 1
    }
}

abstract class F : Java1, KotlinInterface { //Kotlin ← Java, Kotlin2
    override fun bar(o: Any?) {}

    override fun foo(): Int {
        return 1
    }
}

class G : Java1, KotlinInterface, SeparateModuleJava1 { //Kotlin ← Java1, Java2, Kotlin2 (separate module)
    override var a: Int
        get() = TODO("")
        set(value) {}

    override fun foo(): Int {
        return 1
    }

    override fun bar(o: Int) {}

    override fun bar(o: Any?) {}
}

abstract class H : Java1, KotlinInterface, SeparateModuleJava1 { //Kotlin ← Java1, Java2, Kotlin2 (separate module)
    override fun bar(o: Any) {}
}

abstract class I : Java1, Java2, Java3 {    //Kotlin ← Java1, Java2, Java3
    override fun bar(o: Any?) {}
}

class J : Java1, Java2, Java3 { //Kotlin ← Java1, Java2, Java3
    override fun bar(o: Number?) {}

    override fun bar(o: Int) {}

    override fun bar(o: Any?) {}

    override fun foo(): Int {
        return 1
    }
}

interface KotlinInterface {
    var a: Int
    fun foo(): Int
    fun bar(o: Int)
}

interface KotlinInterface2 {
    var a: Any
    fun foo(): Any
    fun bar(o: Any)
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J) {
    val k1: Int = a.foo()
    val k2: Unit = a.bar(1)
    val k3: Unit = a.bar("")
    val k4: Unit = a.bar(null)
    val k5: Int = b.foo()
    val k6: Unit = b.bar(1)
    val k7: Unit = b.bar("")
    val k8: Unit = b.bar(null)
    val k9: Any = Java1.a
    val k10: Int = Java2.a
    val k11: Any = SeparateModuleJava1.a
    val k12: Int = SeparateModuleJava2.a
    val k13: Int = c.foo()
    val k14: Unit = c.bar(1)
    val k15: Unit = c.bar("")
    val k16: Unit = c.bar(null)
    val k17: Int = d.foo()
    val k18: Unit = d.bar(1)
    val k19: Unit = d.bar("")
    val k20: Unit = d.bar(null)
    val k22: Int = e.foo()
    val k23: Unit = e.bar(1)
    val k24: Unit = e.bar("")
    val k25: Unit = e.bar(null)
    val k26: Int = e.a
    e.a = 4
    val k27: Int = f.foo()
    val k28: Unit = f.bar(1)
    val k29: Unit = f.bar("")
    val k31: Int = f.a
    f.a = 4
    val k32: Int = g.foo()
    val k33: Unit = g.bar(1)
    val k34: Unit = g.bar(1.2)
    val k35: Unit = g.bar("")
    val k36: Unit = g.bar(null)
    val k37: Number = Java3.a
    val k38: Int = h.foo()
    val k39: Unit = h.bar(1)
    val k40: Unit = h.bar("")
    val k41: Int = h.a
    h.a = 4
    val k42: Int = i.foo()
    val k43: Unit = i.bar(1)
    val k44: Unit = i.bar("")
    val k45: Unit = i.bar(null)
    val k46: Int = j.foo()
    val k47: Unit = j.bar(1)
    val k48: Unit = j.bar("")
}
