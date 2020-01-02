// FILE: Bad.java

public class Bad<T extends Bad> {}

// FILE: X.java

public class X {
  Bad foo() {return null;}
}

// FILE: Usage.kt

fun foo(p: X) = p.foo()
