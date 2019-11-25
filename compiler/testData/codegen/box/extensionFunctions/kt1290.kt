// IGNORE_BACKEND_FIR: JVM_IR
//KT-1290 Method property in constructor causes NPE

class Foo<T>(val filter: (T) -> Boolean) {
    public fun bar(tee: T) : Boolean {
        return filter(tee);
    }
}

fun foo() = Foo({ i: Int -> i < 5 }).bar(2)

fun box() : String {
    if (!foo()) return "fail"
    return "OK"
}
