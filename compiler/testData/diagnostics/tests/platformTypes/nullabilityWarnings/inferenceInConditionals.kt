// FILE: J.java

import org.jetbrains.annotations.*;
import java.util.*;

public class J {
    @NotNull
    public String nn() { return ""; }

    @Nullable
    public List<String> n() { return null; }
}

// FILE: k.kt

fun safeCall(c: J?) {
  c?.nn()?.length
}

fun ifelse(c: J): Any? {
    return if (true) c.nn() else null
}

fun elvis(c: J): Any? {
    return null ?: c.nn()
}

