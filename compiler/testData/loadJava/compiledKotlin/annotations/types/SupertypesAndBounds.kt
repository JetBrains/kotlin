package test

annotation class A

trait Foo<T : [A] Number> : [A] CharSequence {
    fun <E, F : [A] E> bar()
}
