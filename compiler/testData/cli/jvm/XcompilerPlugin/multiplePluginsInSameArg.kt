package foo

annotation class NoArg
annotation class AllOpen

@AllOpen
class Base(val s: String)

@NoArg
class Derived(s: String) : Base(s)
