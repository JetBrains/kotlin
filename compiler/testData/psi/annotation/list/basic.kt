@[abc("") cde]
class A {
    @[ abc
    cde]
    @[private]
    fun foo() {
        @[data inline] class Local {}

        @[suppress("a")] (1 + @[abc] 3)
    }
}
