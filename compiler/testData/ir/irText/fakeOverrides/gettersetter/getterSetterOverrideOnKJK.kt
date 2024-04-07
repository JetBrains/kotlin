// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-65679, KT-65801

// FILE: Java1.java
public class Java1 extends KotlinClass { }

// FILE: Java2.java
public class Java2 extends KotlinClass {
    int a = 29;
    private int b = 30;
    protected int c = 31;
    public int d = 32;
    int e = 33;
    int f = 34;
    int g = 35;
    int h = 36;
    int i = 37;
    int j = 38;
    int k = 39;
    boolean l = false;
}

// FILE: 1.kt
class A : Java1()

class B : Java1() {
    override val a: Int
        get() = 18

    override var b: Int = 19
        set(value) {
            field = 20
        }

    override val c: Int
        get() = 21

    override var d: Int = 22
        set(value) {
            field = 23
        }

    override var j = 24

    override var k: Int = 25
        get() = 26
        set(value) {
            field = 27
        }
    override var l: Boolean = true
        set(value) {
            field = false
        }
}

class C : Java2()

class D : Java2() {
    override val a: Int
        get() = 41
    override var b: Int = 42
        get() = 43
        set(value) {
            field = value+1
        }
    override val c: Int
        get() = 44
    override var d: Int = 45
        set(value) {
            field = 46
        }
}

open class KotlinClass {
    open val a: Int = 1
    open var b: Int = 2

    open val c : Int
        get() = 3

    open var d : Int = 4
        set(value) {
            field = value + 1
        }

    var e : Int = 5
        get() = 6
        private set(value) {
            field = value + 1
        }

    protected var f : Int  = 7
        get() = 8
        protected set(value) {
            field = value + 1
        }

    private var g : Int = 9
        get() = 10
        private set(value) {
            field = value + 1
        }

    internal var h : Int = 11
        get() = 12
        internal set(value) {
            field = value + 1
        }

    protected var i : Int  = 13
        get() = 14
        private set(value) {
            field = value + 1
        }

    open val j get() = 15

    open var k: Int = 16
        protected set

    open var l: Boolean = false
}

fun test(a: A, b: B, c: C, d: D) {
    a.a
    a.b
    a.c
    a.d
    a.e
    a.j
    a.k
    a.l
    b.a
    b.b
    b.c
    b.d
    b.e
    b.j
    b.k
    b.l
    c.a
    c.b
    c.c
    c.d
    c.e
    c.j
    c.k
    c.l
    d.a
    d.b
    d.c
    d.e
}