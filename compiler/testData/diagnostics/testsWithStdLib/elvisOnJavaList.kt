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
    return <!NI;TYPE_MISMATCH!>c.getList() ?: <!OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH, OI;TYPE_MISMATCH!>listOf()<!><!>
}