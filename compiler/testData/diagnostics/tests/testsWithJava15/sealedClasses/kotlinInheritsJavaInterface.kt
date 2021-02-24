// ISSUE: KT-41215

// FILE: Base.java
public sealed interface Base permits A, B {}

// FILE: A.java
public final class A extends Base {}

// FILE: B.kt

class B : <!CLASS_INHERITS_JAVA_SEALED_CLASS!>Base<!>
