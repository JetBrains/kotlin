//FILE:Main.kt
fun main(args : Array<String>) {
    val inner = Nested().inner()

    println(inner.toString())
}

//FILE:Nested.java
public class Nested {
    public class Inner$ {}

    public Inner$ inner() {
        return  new Inner$();
    }
}
