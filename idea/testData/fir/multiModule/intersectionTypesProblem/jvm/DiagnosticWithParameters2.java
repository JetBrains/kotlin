package jvm;

public interface DiagnosticWithParameters2<E, A, B> extends Diagnostic {
    A getA();
    B getB();
}