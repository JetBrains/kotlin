package test

class A

fun call(a: String, b: String, c: String, d: String, e: String, f: Any) {

}

inline fun <reified T: Any> Any?.foo(): T {
    call("1", "2", "3", "4", "5", this as T)
    return this as T
}

