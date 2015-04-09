package test

annotation class A

class SimpleTypeAnnotation {
    fun foo(x: [A] IntRange): [A] Int = 42
}
