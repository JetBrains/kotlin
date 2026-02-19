// RUN_PIPELINE_TILL: FRONTEND
interface SomeInterface {
    fun foo(x: Int, y: String): String

    val bar: Boolean
}

class SomeClass : SomeInterface {
    private val baz = 42

    override fun foo(x: Int, y: String): String {
        return y + x + baz
    }

    override var bar: Boolean
        get() = true
        set(value) {}

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var fau: Double
}

<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, INLINE_CLASS_DEPRECATED!>inline<!> class InlineClass

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, getter, integerLiteral,
interfaceDeclaration, lateinit, override, propertyDeclaration, setter */
