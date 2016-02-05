// FILE: P.java

import java.util.ArrayList;
import java.util.List;

public class P {
    public List<Integer> getList() {
        return new ArrayList<Integer>();
    }
}

// FILE: Test.kt

fun foo(c: P): MutableList<Int> {
    // Error should be here: see KT-8168 Typechecker fails for platform collection type
    return c.getList() ?: <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH, TYPE_MISMATCH!>listOf()<!>
}