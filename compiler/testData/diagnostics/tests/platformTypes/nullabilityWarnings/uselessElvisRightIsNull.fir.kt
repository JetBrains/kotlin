// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @Nullable
    public static J staticN;
}

// FILE: JJ.java

public class JJ {
    public static JJ staticNN;
}

// FILE: JJJ.java

import org.jetbrains.annotations.*;

public class JJJ {
    @NotNull
    public static JJJ staticNNN;
}

// FILE: JJJJ.java

public interface JJJJ<R> {
    R get();
}

// FILE: k.kt

fun test() {
    val a = J.staticN <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>
    foo(a)
    val b = JJ.staticNN <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>
    foo(b)
    val c = JJJ.staticNNN <!USELESS_ELVIS!>?: null<!>
    foo(c)
}

fun foo(a: Any?) {
}

fun <R> test2(j: JJJJ<R>) = j.get() <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>
