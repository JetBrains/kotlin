// FILE: Element.java
// FULL_JDK

public interface Element {}

// FILE: DerivedElement.java

public interface DerivedElement extends Element {}

// FILE: EmptyDiagnostic.java

public class EmptyDiagnostic {}

// FILE: Diagnostic.java

import org.jetbrains.annotations.NotNull;

public class Diagnostic<E extends Element> extends EmptyDiagnostic {
    @NotNull
    public E getElement();
}

// FILE: DiagnosticFactory.java

import org.jetbrains.annotations.NotNull;

public class DiagnosticFactory<D extends EmptyDiagnostic> {
    @NotNull
    public D cast(@NotNull EmptyDiagnostic diagnostic) {
        return (D) diagnostic;
    }
}

// FILE: DiagnosticFactory0.java
public class DiagnosticFactory0<E extends Element> extends DiagnosticFactory<Diagnostic<E>> {}

// FILE: test.kt

class Fix(e: DerivedElement)

fun create(d: Diagnostic<DerivedElement>) {
    val element = d.element
    Fix(element)
}

fun <DE : DerivedElement> createGeneric(d: Diagnostic<DE>) {
    val element = d.element
    Fix(element)
}

private val DERIVED_FACTORY = DiagnosticFactory0<DerivedElement>()

fun createViaFactory(d: EmptyDiagnostic) {
    val casted = DERIVED_FACTORY.cast(d)
    val element = casted.element
    Fix(element)
}
