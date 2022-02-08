// !LANGUAGE: -SuspendFunctionAsSupertype
typealias Action = () -> Unit

interface SAM {
    fun run()
}

typealias Test1 = suspend () -> Unit
typealias Test2 = suspend Int.() -> Unit
typealias Test3 = <!WRONG_MODIFIER_TARGET!>suspend<!> Function0<Unit>
typealias Test4 = <!WRONG_MODIFIER_TARGET!>suspend<!> Action
typealias Test5 = List<suspend () -> Unit>
typealias Test6 = <!WRONG_MODIFIER_TARGET!>suspend<!> List<() -> Unit>
typealias Test7 = <!WRONG_MODIFIER_TARGET!>suspend<!> SAM
typealias Test8 = <!WRONG_MODIFIER_TARGET!>suspend<!> kotlin.coroutines.SuspendFunction0<Unit>
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
typealias Test19 = (@A(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>{ val x: <!WRONG_MODIFIER_TARGET!>suspend<!> String? = null; "" }()<!>) suspend () -> Unit)?
typealias Test20 = (@A(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"".let { val x: <!WRONG_MODIFIER_TARGET!>suspend<!> String? = null; it }<!>) suspend () -> Unit)?

interface Supertype1 : suspend () -> Unit {

}

interface Supertype2 : <!SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE!>suspend String.() -> Unit<!> {

}

@Target(AnnotationTarget.TYPE)
annotation class A(val value: String = "")

interface RS
