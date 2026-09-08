// RUN_PIPELINE_TILL: BACKEND
// An inner class of a generic outer, named by its simple name from a class which neither encloses nor
// inherits it. Nothing supplies `Outer`'s `T` there, so the reference is raw, as the qualified
// `Outer.Inner` spelling is; javac reports `[rawtypes]` for both.
// The raw type erases `T`, which is what the type of `get()` is checked for: the calls of `put` accept
// any argument under erasure as well as under an unresolved `T`.

// FILE: p/Outer.java
package p;

public class Outer<T> {
    public class Inner {
        public T get() { return null; }
        public void put(T t) {}
    }
}

// FILE: p/Unrelated.java
package p;

import p.Outer.Inner;

public class Unrelated {
    public Inner viaImport() { return null; }
    public Outer.Inner viaQualified() { return null; }
}

// FILE: test.kt
import p.Unrelated

fun test(u: Unrelated) {
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Any..kotlin.Any?)")!>u.viaImport().get()<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Any..kotlin.Any?)")!>u.viaQualified().get()<!>

    u.viaImport().put(1)
    u.viaImport().put("")
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, javaFunction, javaType, stringLiteral */
