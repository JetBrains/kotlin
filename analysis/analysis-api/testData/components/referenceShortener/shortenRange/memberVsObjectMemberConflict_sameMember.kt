package test

interface Interface {
    fun action() {}
}

object Obj : Interface {}

class Cls : Interface {
    fun test() {
        <expr>Obj.action()</expr>
    }
}