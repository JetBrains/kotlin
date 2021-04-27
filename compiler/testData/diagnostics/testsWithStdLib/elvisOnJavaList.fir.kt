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
    return <!RETURN_TYPE_MISMATCH!>c.getList() ?: <!TYPE_MISMATCH!>listOf()<!><!>
}
