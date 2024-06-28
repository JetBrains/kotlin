// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR

// FILE: Java1.java
import org.jetbrains.annotations.NotNull;

public abstract class Java1 extends A {
    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return null;
    }
}

// FILE: Java2.java
public interface Java2 extends CharSequence { }

// FILE: 1.kt
import java.util.stream.IntStream

abstract class A : CharSequence

abstract class B : Java1()  //Kotlin ← Java ← Kotlin ← Kotlin

class C(override val length: Int) : B() {
    override fun get(index: Int): Char {
        return '1'
    }
}

abstract class D : Java1(), Java2   //Kotlin ← Java1, Java2 ← Kotlin2

class E : Java1(), Java2 {
    override val length: Int
        get() = 10

    override fun get(index: Int): Char {
        return '1'
    }
}

abstract class F(override val length: Int) : java.lang.CharSequence, CharSequence, KotlinInterface {    //Kotlin ← Java, Kotlin1, Kotlin2
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return null!!
    }

    override fun chars(): IntStream {
        return null!!
    }

    override fun codePoints(): IntStream {
        return null!!
    }

    override fun get(index: Int): Char {
        return '1'
    }
}

abstract class G : Java1(), KotlinInterface // Kotlin ← Java, Kotlin2 ← Kotlin3

class H : Java1(), KotlinInterface {
    override val length: Int
        get() = 10
    override fun get(index: Int): Char {
        return '1'
    }
    override operator fun get(index: Any): Char {
        return '2'
    }
}

abstract class I(override val length: Int) : java.lang.CharSequence, CharSequence {
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return null!!
    }

    override fun chars(): IntStream {
        return null!!
    }

    override fun codePoints(): IntStream {
        return null!!
    }

    override fun get(index: Int): Char {
        return 'a'
    }
}

interface KotlinInterface {
    fun get(index: Any): Char
}

fun test(b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I){
    b.length
    b[1]
    b.get(1)
    b.subSequence(1,2)

    c.length
    c[1]
    c.subSequence(1,2)

    d.length
    d[1]
    d.chars()

    e.length
    e[1]
    e.subSequence(1,2)

    f.length
    f.length()
    f[1]
    f.get(1.2)

    g.length
    g[1]
    g.subSequence(1,2)

    h.length
    h[1]
    h[1.2]
    h.subSequence(1,2)

    i.length
    i.length()
    i.subSequence(1,2)
    i[1]
}