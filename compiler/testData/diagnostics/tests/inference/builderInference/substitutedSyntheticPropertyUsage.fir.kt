// FILE: GenericJava.java
public class GenericJava<F> {
    public java.util.List<F> getFoo() {}
}

// FILE: main.kt
class Controller<T> {
    fun yield(t: T) {}

    fun gg(): GenericJava<T> = TODO()
}

fun <S> generate(g: suspend Controller<S>.() -> Unit) {}

fun main() {
    generate {
        yield("")
        gg().foo
    }
}