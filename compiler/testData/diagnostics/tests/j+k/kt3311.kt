// FILE: Super.java
public class Super {
    public boolean foo;
    public boolean bar;

    public void setFoo(boolean foo) {
        this.foo = foo;
    }
}

// FILE: b.kt
public class Sub: Super() {
}

fun main(args: Array<String>) {
    val x = Sub()
    x.foo = true
    x.bar = true
}
