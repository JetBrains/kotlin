// FILE: A.java

class A {
    public static void main(String[] args) {
        args[0] = "OK";
    }
}

// FILE: 1.kt

fun box(): String {
    val args = arrayOf("Fail")
    (A::main)(args)
    return args[0]
}
