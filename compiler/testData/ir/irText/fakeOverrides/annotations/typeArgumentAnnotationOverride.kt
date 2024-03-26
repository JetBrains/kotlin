// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: Java1.java
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.Set;

public class Java1 {
    public Set<@NotNull String> toSet(Collection<@NotNull String> elements) {
        return null;
    }
}

// FILE: Java2.java
public class Java2 extends Java1 { }

// FILE: Java3.java
import java.util.Collection;
import java.util.Set;

interface Java3 {
    Set<String> toSet(Collection<String> elements);
}

// FILE: Java4.java
public class Java4 extends A { }

// FILE: Java5.java
import java.util.Collection;
import java.util.Set;

public class Java5 extends A {
    @Override
    public Set<String> toSet(Collection<String> elements) {
        return null;
    }
}

// FILE: 1.kt
open class A : Java1()

class B : Java2()

class C : Java1(), Java3

class D : Java1() , KotlinInterface

class E : Java4()

class F : Java5()

interface KotlinInterface{
    fun toSet(elements: Collection<String>): Set<String?>
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F) {
    val k: Set<String> = a.toSet(mutableListOf("1"))
    val k2: Set<String> = b.toSet(mutableListOf("1"))
    val k3: Set<String> = c.toSet(mutableListOf("1"))
    val k4: Set<String> = d.toSet(mutableListOf("1"))
    val k5: Set<String> = e.toSet(mutableListOf("1"))
    val k6: Set<String> = f.toSet(mutableListOf("1"))
}