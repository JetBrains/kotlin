// FILE: JavaClass.java
public class JavaClass<T> {
    public JavaClass(T t) {}

    public T foo() {}
}

// FILE: main.kt
fun main() {
    // See https://github.com/JetBrains/kotlin/commit/437a26684d3529ee2cfdbe54e59d50f4a6f0a611#diff-ba68311bbe28b71196c36a6246000382L176
    // In simplifyLowerConstraint, we add constraint Nothing? <: T!, approximating it with Nothing? <: T
    // But before it we already had T <: String from the explicit types
    // That lead us to constraint contradiction: Nothing? !<: String
    // We need to discuss the commit above at some point
    <!INAPPLICABLE_CANDIDATE!>JavaClass<!><String>(null).<!UNRESOLVED_REFERENCE!>foo<!>().<!UNRESOLVED_REFERENCE!>length<!>
}
