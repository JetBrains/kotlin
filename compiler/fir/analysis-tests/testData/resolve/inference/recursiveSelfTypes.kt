// ISSUE: KT-52860

abstract class Child<R : Recursive<R>> : Parent<ChildParameter<*>, R> {
    fun getParent(): Child<R> = self() as Child<R>
}

interface Parent<P : Parameter, R : Recursive<R>> {
    fun self(): Parent<P, R>
}

interface Parameter

class ChildParameter<R : Recursive<R>> : Parameter

interface Recursive<R : Recursive<R>>
