// FILE: JavaEnum.java
public enum JavaEnum {
    A, B, C;

    public String getName() {
        return "FromJava";
    }
}

// FILE: Usage.kt
fun foo() {
    JavaEnum.A.get<caret>Name()
}
