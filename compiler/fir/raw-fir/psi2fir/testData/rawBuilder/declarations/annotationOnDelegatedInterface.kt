// ISSUE: KT-70854

interface Base {
    fun print()
}

class BaseImpl(val x: Int) : Base {
    override fun print() {}
}

@Target(AnnotationTarget.EXPRESSION)
annotation class Some(val s: String)

class Derived(b: Base) : Base by @Some("Anything") b
