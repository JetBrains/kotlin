// FILE: J.java

import org.jetbrains.annotations.*;
import java.util.*;

public class J {
    @Nullable
    public List<String> n() { return null; }
}

// FILE: k.kt

fun list(j: J): Any {
    val a = j.n()!!

    a?.get(0)
    if (a == null) {}
    a!!

    a.get(0)
    return a
}
