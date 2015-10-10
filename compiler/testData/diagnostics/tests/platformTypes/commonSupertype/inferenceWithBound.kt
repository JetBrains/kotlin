// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: p/Super.java

package p;

public interface Super {

}

// FILE: p/Sub.java

package p;

public interface Sub extends Super {}

// FILE: p/Other.java

package p;

import java.util.*;

public class Other {

    public static Sub sub;

    public static Collection<Sub> subs;
    public static Collection<Super> supers;
}

// FILE: k.kt

import p.*

fun test() {
    val col = if (1 < 2) Other.subs else Other.supers
    col.foo()
}

fun <T: Super> Collection<T>.foo(): T = null!!
fun <T> listOf(t: T): List<T> = null!!