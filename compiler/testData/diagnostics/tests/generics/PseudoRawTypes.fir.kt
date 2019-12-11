// JAVAC_EXPECTED_FILE
// FILE: java/util/Collection.java
package java.util;

public class Collection {
  public void foo() {}
}

// FILE: test/Usage.java
package test;

import java.util.*;

public class Usage {
  void foo(Collection c) {
    c.foo();
  }
}

// FILE: Kotlin.kt
package test

fun foo(u: Usage) {
  u.foo(null)
}
