// !DIAGNOSTICS: -UNUSED_PARAMETER -NOTHING_TO_INLINE
class Controller

fun valid(<info descr="null">coroutine</info> <warning descr="[UNUSED_PARAMETER] Parameter 'c' is never used">c</warning>: Controller.() -> Continuation<Unit>) {

}

fun noFunctionType(<error descr="[INAPPLICABLE_MODIFIER] 'coroutine' modifier is inapplicable. The reason is that parameter should have function type with extension like 'Controller.() -> Continuation<Unit>'"><info descr="null">coroutine</info></error> <warning descr="[UNUSED_PARAMETER] Parameter 'c' is never used">c</warning>: Unit) {

}

fun noExtensionFunction(<error descr="[INAPPLICABLE_MODIFIER] 'coroutine' modifier is inapplicable. The reason is that parameter should have function type with extension like 'Controller.() -> Continuation<Unit>'"><info descr="null">coroutine</info></error> <warning descr="[UNUSED_PARAMETER] Parameter 'c' is never used">c</warning>: (Controller) -> Continuation<Unit>) {

}

fun nullableReturnType(<error descr="[INAPPLICABLE_MODIFIER] 'coroutine' modifier is inapplicable. The reason is that parameter should have function type like 'Controller.() -> Continuation<Unit>' (Continuation<Unit> for return type is necessary)"><info descr="null">coroutine</info></error> <warning descr="[UNUSED_PARAMETER] Parameter 'c' is never used">c</warning>: Controller.() -> Continuation<Unit>?) {

}

fun wrongReturnType(<error descr="[INAPPLICABLE_MODIFIER] 'coroutine' modifier is inapplicable. The reason is that parameter should have function type like 'Controller.() -> Continuation<Unit>' (Continuation<Unit> for return type is necessary)"><info descr="null">coroutine</info></error> <warning descr="[UNUSED_PARAMETER] Parameter 'c' is never used">c</warning>: Controller.() -> Collection<Unit>) {

}

fun notUnitContinuation(<error descr="[INAPPLICABLE_MODIFIER] 'coroutine' modifier is inapplicable. The reason is that parameter should have function type like 'Controller.() -> Continuation<Unit>' (Continuation<Unit> for return type is necessary)"><info descr="null">coroutine</info></error> <warning descr="[UNUSED_PARAMETER] Parameter 'c' is never used">c</warning>: Controller.() -> Continuation<Int>) {

}

<info descr="null">inline</info> fun inlineBuilder(<error descr="[INAPPLICABLE_MODIFIER] 'coroutine' modifier is inapplicable. The reason is that coroutine parameter of inline function should be marked as 'noinline'"><info descr="null">coroutine</info></error> <warning descr="[UNUSED_PARAMETER] Parameter 'c' is never used">c</warning>: Controller.() -> Continuation<Unit>) {

}

<warning descr="[NOTHING_TO_INLINE] Expected performance impact of inlining 'public inline fun inlineBuilderNoInlineCoroutine(noinline coroutine c: Controller.() -> Continuation<Unit>): Unit defined in root package' can be insignificant. Inlining works best for functions with lambda parameters"><info descr="null">inline</info></warning> fun inlineBuilderNoInline<TYPO descr="Typo: In word 'Coroutine'">Coroutine</TYPO>(<info descr="null">coroutine</info> <info descr="null">noinline</info> <warning descr="[UNUSED_PARAMETER] Parameter 'c' is never used">c</warning>: Controller.() -> Continuation<Unit>) {

}
