package foo

annotation class fancy

[fancy]
class Foo {
    [fancy]
    fun baz([fancy] foo : Int) : Int {
        return [fancy] 1
    }
}
