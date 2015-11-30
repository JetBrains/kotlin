// FILE: A.java

import java.util.*;

public interface A {
    <R> void foo();
}

// FILE: B.java

public interface B extends A {}

// FILE: k.kt

class C(x: A) : A by x, B {}
