// FILE: Bad.java

class Bad<T extends Bad> {}

// FILE: X.java

class X {
  Bad foo() {return null;}
}

// FILE: Usage.kt

fun foo(p: X) = p.foo()
