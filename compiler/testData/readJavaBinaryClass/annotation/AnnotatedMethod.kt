package test

annotation class Aaa

open class HasAnnotatedMethod() : java.lang.Object() {
    open Aaa fun f(): Unit { }
}
