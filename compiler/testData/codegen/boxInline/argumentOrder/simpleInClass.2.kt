package test

class Z(val p: String) {
     inline fun test(a: String, b: String, c: () -> String): String {
        return a + b + c() + p;
    }
}