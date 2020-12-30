// !WITH_NEW_INFERENCE

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
    return <!TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!>c.getList() ?: <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}, TYPE_MISMATCH{OI}!>listOf()<!><!>
}
