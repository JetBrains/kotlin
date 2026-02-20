// RUN_PIPELINE_TILL: FRONTEND
class C<T>(
    val foo: String
) {
    fun bar() {}
    inner class Inner

    class Nested

    companion object {
        fun companionFun() {}
    }

    companion {
        fun test(t: T) {
            <!UNRESOLVED_REFERENCE!>foo<!>
            <!UNRESOLVED_REFERENCE!>bar<!>()
            <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()

            Nested()
            companionFun()
        }

        val testProp: T?
            get() {
                <!UNRESOLVED_REFERENCE!>foo<!>
                <!UNRESOLVED_REFERENCE!>bar<!>()
                <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()

                Nested()
                companionFun()
                return null
            }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, inner, nestedClass, nullableType,
objectDeclaration, primaryConstructor, propertyDeclaration, typeParameter */
