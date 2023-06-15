// TARGET_BACKEND: JVM

// FILE: javaWildcardType.kt
interface K {
    fun kf1(): Collection<out CharSequence>
    fun kf2(): Collection<CharSequence>

    fun kg1(c: Collection<out CharSequence>)
    fun kg2(c: Collection<CharSequence>)
}


class C(j: J, k: K) : J by j, K by k

// FILE: J.java
import java.util.*;

public interface J {
    Collection<? extends CharSequence> jf1();
    Collection<CharSequence> jf2();

    void jg1(Collection<? extends CharSequence> c);
    void jg2(Collection<CharSequence> c);
}
