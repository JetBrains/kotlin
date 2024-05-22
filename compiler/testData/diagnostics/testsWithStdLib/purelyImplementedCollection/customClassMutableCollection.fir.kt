// !DIAGNOSTICS: -UNUSED_VARIABLE
// JAVAC_EXPECTED_FILE

import java.util.*;

// FILE: A.java
@kotlin.jvm.PurelyImplements("kotlin.collections.MutableCollection")
class A<T> extends AbstractCollection<T> {
    @Override
    public Iterator<T> iterator() {
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

    val b1: Collection<String?> = x
    val b2: MutableCollection<String?> = <!INITIALIZER_TYPE_MISMATCH!>x<!>
}
