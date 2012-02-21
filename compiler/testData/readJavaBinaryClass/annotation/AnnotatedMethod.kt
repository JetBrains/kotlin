package test

annotation class Aaa

open class HasAnnotatedMethod() {
    open Aaa fun f(): Unit { }
}
