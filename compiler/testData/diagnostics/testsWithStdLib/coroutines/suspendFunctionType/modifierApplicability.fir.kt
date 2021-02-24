typealias Action = () -> Unit

interface SAM {
    fun run()
}

typealias Test1 = suspend () -> Unit
typealias Test2 = suspend Int.() -> Unit
typealias Test3 = suspend Function0<Unit>
typealias Test4 = suspend Action
typealias Test5 = List<suspend () -> Unit>
typealias Test6 = suspend List<() -> Unit>
typealias Test7 = suspend SAM
typealias Test8 = suspend kotlin.coroutines.SuspendFunction0<Unit>
typealias Test9 = suspend (() -> Unit) -> Unit
typealias Test10 = suspend (suspend () -> Unit) -> Unit
typealias Test11 = suspend () -> (suspend () -> Unit)
typealias Test12 = suspend (suspend (() -> Unit)) -> Unit
typealias Test13 = @A() suspend () -> Unit
typealias Test14 = @A suspend () -> Unit
typealias Test15 = (@A() suspend () -> Unit)?
typealias Test16 = (@A suspend () -> Unit)?
typealias Test17 = @A suspend RS.() -> Unit
typealias Test18 = (suspend () -> Unit)?

interface Supertype1 : suspend () -> Unit {

}

interface Supertype2 : suspend String.() -> Unit {

}

@Target(AnnotationTarget.TYPE)
annotation class A

interface RS
