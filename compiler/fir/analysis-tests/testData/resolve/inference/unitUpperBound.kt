// WITH_STDLIB
// FULL_JDK

class A {
    fun foo(x: List<String>) {
        var w = 1
        x.ifEmpty {
            w += 2
        }
    }
}
