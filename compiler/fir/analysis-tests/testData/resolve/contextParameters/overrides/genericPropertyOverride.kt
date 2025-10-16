// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-63246

abstract class Base<T> {
    context(r: R)
    abstract fun <R> String.foo(): Int?

    context(r: R)
    abstract fun <R> T.foo(t: T): Int?

    context(r: R)
    abstract val <R> String.bar: Int?

    context(r: R)
    abstract val <R> T.baz: Int?
}

class Child : Base<String>() {
    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>child<!>: RChild)
    override fun <RChild> String.foo(): Int? = 1

    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>child<!>: RChild)
    override fun <RChild> String.foo(t: String): Int? = 1

    context(child: RChild)
    override val <RChild> String.bar: Int? get() = 1

    context(child: RChild)
    override val <RChild> String.baz: Int? get() = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
getter, integerLiteral, nullableType, override, propertyDeclaration, propertyDeclarationWithContext,
propertyWithExtensionReceiver, typeParameter */
