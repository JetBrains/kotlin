// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE

// FILE: p/Rec.java

package p;

public interface Rec<T> {

}

// FILE: p/A.java

package p;

public interface A extends Rec<A> {

}

// FILE: p/B.java

package p;

public interface B extends Rec<B> {

}

// FILE: k.kt

import p.*

fun test(a: A, b: B, c: Boolean) {
    var ab = if (c) a else b
    ab = a
    ab = b
}