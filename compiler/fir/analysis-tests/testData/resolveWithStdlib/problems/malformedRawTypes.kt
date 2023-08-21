// FILE: Generic.java

public class Generic<T> {
    public class Inner {}
    public static Generic raw = new Generic<>();
    public Inner inner = new Inner();
    public Generic.Inner rawInner = new Inner();
}

// FILE: Main.kt
fun main() {
    var generic = Generic.raw

    var inner1 = generic.inner
    inner1 = Generic<String>().Inner()

    var inner2 = generic.rawInner
    inner2 = Generic<String>().Inner()
}
