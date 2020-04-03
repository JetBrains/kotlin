// FILE: A.java

public class A {
    public String getFoo() {
        return "Foo";
    }
}

// FILE: B.kt

class B(private val foo: String) : A() {
    override fun getFoo(): String = foo
}
