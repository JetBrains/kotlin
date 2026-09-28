// FILE: parent.kt
interface Parent {
    fun visit(visitor: (Parent) -> Unit) {
        visitor.invoke(this)
    }
}

// FILE: Child.java
public interface Child extends Parent {
    int intValue();
}

// FILE: test.kt
fun box(): String {
    var result = "fail"
    Child { 42 }.visit { result = "OK" }
    return result
}
