// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +ImprovedResolutionInSecondaryConstructors
// DIAGNOSTICS: -UNUSED_PARAMETER
open class Base(p: Any?) {
    fun foo1() {}
}

fun Base.foo() {
    class B : Base {
        constructor() : super(foo1())
        constructor(x: Int) : super(this@foo.foo1())
        constructor(x: Int, y: Int) : super(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@B<!>.foo1())
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, localClass, nullableType,
primaryConstructor, secondaryConstructor, thisExpression */
