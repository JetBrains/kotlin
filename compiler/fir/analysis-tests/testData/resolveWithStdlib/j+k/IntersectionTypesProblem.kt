// FILE: jvm/Diagnostic.java

package jvm;

public interface Diagnostic {}

// FILE: jvm/DiagnosticWithParameters1.java

package jvm;

public interface DiagnosticWithParameters1<E, A> extends Diagnostic {
    A getA();
}

// FILE: jvm/DiagnosticWithParameters2.java

package jvm;

public interface DiagnosticWithParameters2<E, A, B> extends Diagnostic {
    A getA();
    B getB();
}

// FILE: test.kt

package jvm;

fun <K> select(x: K, y: K): K = x

fun test(d1: DiagnosticWithParameters1<*, *>, d2: DiagnosticWithParameters2<*, *, *>) {
    val res = select(d1.a, d2.b)
}
