// FILE: Base.java

interface Interface {
    String call(String t);
}

// FILE: 1.kt

val String.property: String
    get() = this

fun box(): String {
    return Interface(String::property).call("OK")
}
