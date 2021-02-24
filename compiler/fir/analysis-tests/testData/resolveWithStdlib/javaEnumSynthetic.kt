// FILE: JavaEnum.java
public enum JavaEnum {
    X, Y
}

// FILE: main.kt
fun foo() {
    JavaEnum.values()
    JavaEnum.valueOf("")
}
