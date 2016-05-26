// !DIAGNOSTICS: -UNUSED_PARAMETER -NOTHING_TO_INLINE
class Controller

class A(coroutine c: Controller.() -> Continuation<Unit>, y: Int) {
    var x: String = ""
        set(<!INAPPLICABLE_MODIFIER!>coroutine<!> x) {

        }

    constructor(coroutine c: Controller.() -> Continuation<Unit>) : this(c, 1)
    constructor(<!INAPPLICABLE_MODIFIER!>coroutine<!> c: Controller.() -> Continuation<Nothing>, y: String) : <!NONE_APPLICABLE!>this<!>(c, 2)
}

fun valid(coroutine c: Controller.() -> Continuation<Unit>) {

}

fun noFunctionType(<!INAPPLICABLE_MODIFIER!>coroutine<!> c: Unit) {

}

fun noExtensionFunction(<!INAPPLICABLE_MODIFIER!>coroutine<!> c: (Controller) -> Continuation<Unit>) {

}

fun nullableReturnType(<!INAPPLICABLE_MODIFIER!>coroutine<!> c: Controller.() -> Continuation<Unit>?) {

}

fun wrongReturnType(<!INAPPLICABLE_MODIFIER!>coroutine<!> c: Controller.() -> Collection<Unit>) {

}

fun notUnitContinuation(<!INAPPLICABLE_MODIFIER!>coroutine<!> c: Controller.() -> Continuation<Int>) {

}

inline fun inlineBuilder(<!INAPPLICABLE_MODIFIER!>coroutine<!> c: Controller.() -> Continuation<Unit>) {

}

inline fun inlineBuilderNoInlineCoroutine(coroutine noinline c: Controller.() -> Continuation<Unit>) {

}

