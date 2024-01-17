// !DIAGNOSTICS: -UNUSED_VARIABLE
// JAVAC_EXPECTED_FILE

import java.util.*;

// FILE: A.java
@kotlin.jvm.PurelyImplements("kotlin.collections.MutableList")
class A<T> extends AbstractList<T> {
    @Override
    public T get(int index) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }
}

// FILE: b.kt

fun bar(): String? = null

fun foo() {
    var x = A<String>()
    x.add(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.add(<!ARGUMENT_TYPE_MISMATCH!>bar()<!>)
    x.add("")

    x[0] = <!NULL_FOR_NONNULL_TYPE!>null<!>
    x[0] = <!ARGUMENT_TYPE_MISMATCH!>bar()<!>
    x[0] = ""

    val b1: MutableList<String?> = <!INITIALIZER_TYPE_MISMATCH!>x<!>
    val b2: MutableList<String> = x
    val b3: List<String?> = x

    val b4: Collection<String?> = x
    val b6: MutableCollection<String?> = <!INITIALIZER_TYPE_MISMATCH!>x<!>
}
