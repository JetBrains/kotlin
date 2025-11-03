// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// FIR_DUMP
// LANGUAGE: +ContextParameters

interface Foo

context(t: T)
fun <T : Foo> test() = t

val functional: context(String, Foo) () -> Foo = { contextOf<Foo>() }

open class Base(f: Foo)

object O : Foo {
    class FooImpl : Base(test()), Foo {
        class Bar {
            fun bar() {
                val o: O = test()
                with("") { functional() }
            }
        }
    }
}

class FooImpl2 : Base, Foo {
    constructor() : super(<!NO_CONTEXT_ARGUMENT!>test<!>())
    constructor(s: String) : super(with(s) { <!NO_CONTEXT_ARGUMENT!>functional<!>() })

    class Bar {
        fun bar() {
            <!CANNOT_INFER_PARAMETER_TYPE, NO_CONTEXT_ARGUMENT!>test<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, interfaceDeclaration,
localProperty, nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration, secondaryConstructor,
typeConstraint, typeParameter */
