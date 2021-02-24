// ISSUE: KT-41215

// FILE: Base.java
public sealed class Base permits A, B {}

// FILE: A.java
public final class A extends Base {}

// FILE: B.kt

class B : Base()
