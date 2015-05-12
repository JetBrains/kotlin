// "class org.jetbrains.kotlin.idea.quickfix.RenameParameterToMatchOverriddenMethodFix" "false"

abstract class A {
    abstract fun foo(arg : Int) : Int;
}

interface X {
    fun foo(agr: Int) : Int;
}

class B : A(), X {
    override fun foo(arg<caret>: Int) : Int {
        val x = arg + arg
        return arg
    }
}
