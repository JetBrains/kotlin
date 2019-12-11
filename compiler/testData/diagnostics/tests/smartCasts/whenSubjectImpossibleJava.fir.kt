// See KT-10061
// FILE: My.java
public class My {
    String getSomething() { return "xyz"; }
}

// FILE: My.kt
fun foo(my: My) {
    my.something!!
    when (my.something) { }
}