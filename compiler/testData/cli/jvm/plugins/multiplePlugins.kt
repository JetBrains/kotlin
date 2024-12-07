package foo

annotation class NoArg
annotation class AllOpen

@AllOpen
class Base(val s: String)

class Derived(s: String) : Base(s) {
    @NoArg
    inner class Inner(val s: String)
}
