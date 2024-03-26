// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR

// FILE: Java1.java
import org.jetbrains.annotations.NotNull;

public class Java1 implements CharSequence {

    @Override
    public int length() {
        return 0;
    }

    @Override
    public char charAt(int index) {
        return 0;
    }

    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return null;
    }
}

// FILE: Java2.java
import java.util.stream.IntStream;

public interface Java2  {
    public IntStream codePoints();
}

// FILE: Java3.java
import java.util.stream.IntStream;

public interface Java3  {
    public String toString();
    public IntStream codePoints();
}

// FILE: Java4.java
public abstract class Java4 implements A { }

// FILE: 1.kt
import java.util.stream.IntStream

interface A : java.lang.CharSequence   //Kotlin ← Java

class B : A {
    override fun length(): Int {
        return 1
    }

    override fun charAt(index: Int): Char {
        return '1'
    }

    override fun subSequence(start: Int, end: Int): CharSequence {
        return null!!
    }
}

class C : Java1()   //Kotlin ← Java1 ← Java2

class D : Java1() {
    override val length: Int
        get() = 10

    override fun get(index: Int): Char {
        return 'a'
    }
}

class E : Java1(), Java2 {  //Kotlin ← Java1, Java2 ← Java3
    override fun codePoints(): IntStream {
        return null!!
    }
}

class F : Java1(), KotlinInterface {  //Kotlin ← Java, Kotlin2
    override operator fun get(index: Number): Char {
        return 'a'
    }
}

class G : Java1(), KotlinInterface {
    override fun get(index: Int): Char {
        return 'a'
    }
    override operator fun get(index: Number): Char {
        return 'b'
    }
}

class H : Java1(), Java2, KotlinInterface {  //Kotlin ← Java1, Java2, Kotlin2
    override operator fun get(index: Number): Char {
        return 'a'
    }

    override fun codePoints(): IntStream {
        return null!!
    }
}

class I : Java1(), Java2, Java3 {   //Kotlin ← Java1, Java2, Java3
    override fun codePoints(): IntStream {
        return null!!
    }

}

abstract class J : Java4()   // Kotlin ← Java ← Kotlin ← Java

class K : Java4() {
    override fun length(): Int {
        return 1
    }

    override fun charAt(index: Int): Char {
        return 'a'
    }

    override fun subSequence(start: Int, end: Int): CharSequence {
        return null!!
    }
}

abstract class L : Java1(), A {         // Kotlin ← Java, Kotlin2 ← Java2, Java3
    override fun chars(): IntStream {
        return null!!
    }
    override fun codePoints(): IntStream {
        return null!!
    }
}

abstract class M : Java1(), java.lang.CharSequence {    //Kotlin ← Java1, Java2
    override fun chars(): IntStream {
        return null!!
    }

    override fun codePoints(): IntStream {
        return null!!
    }
}

interface KotlinInterface {
    val length: Number
    fun get(index: Number): Char
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M) {
    a.length()
    a.charAt(1)
    a.chars()
    a.toString()

    b.length()
    b.charAt(1)
    b.subSequence(1,2)

    c.length
    c[1]
    c.chars()

    d.length
    d[1]
    d.isEmpty()

    e.length
    e[1]
    e.isEmpty()
    e.codePoints()

    f.length
    f.isEmpty()
    f[1]

    g.length
    g[1]

    h.length
    h[1]
    h.codePoints()

    i.length
    i.isEmpty()
    i[1]
    i.codePoints()

    j.length()
    j.charAt(1)

    k.charAt(1)
    k.length()

    l.length
    l[1]
    l.length()

    m.length
    m.length()
    m[1]
    m.get(1)
}