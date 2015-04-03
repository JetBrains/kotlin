// FILE: Bar.java
public class Bar {
    @Nullable
    public String find() { return "abc"; }
}

// FILE: Baz.kt
fun foo(bar: Bar): Int? {
    return bar.find()?.length()
}