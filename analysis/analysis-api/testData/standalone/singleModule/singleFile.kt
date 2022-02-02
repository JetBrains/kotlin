package test

annotation class MyAnno

@<caret>MyAnno
class Foo {
    fun bar(): Int = 42
}
