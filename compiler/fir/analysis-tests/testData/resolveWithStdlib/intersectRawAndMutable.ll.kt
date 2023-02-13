// LL_FIR_DIVERGENCE
// Workaround for KT-56630
// LL_FIR_DIVERGENCE
// FILE: Producer.java

import java.util.*;
import org.jetbrains.annotations.*;

public class Producer {
    @NotNull
    public static ArrayList foo() { return null; }
}

// FILE: test.kt

interface StringSet : MutableSet<String>

fun foo(arg: Boolean) {
    val x = Producer.foo()
    if (x is Set<*>) {
        val y = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<*>..java.util.ArrayList<*> & kotlin.collections.Set<*> & java.util.ArrayList<*>..java.util.ArrayList<*>")!>x<!>
    }
    if (x is MutableSet<*>) {
        val y = <!DEBUG_INFO_EXPRESSION_TYPE("java.util.ArrayList<*>..java.util.ArrayList<*> & kotlin.collections.MutableSet<*> & java.util.ArrayList<*>..java.util.ArrayList<*>")!>x<!>
    }
    if (x is StringSet) {
        x.add("")
        x.add(1)
        x.add(null)
        x.iterator().next().length
    }
}
