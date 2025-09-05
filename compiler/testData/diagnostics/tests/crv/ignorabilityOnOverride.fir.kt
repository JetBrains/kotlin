// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

interface Base {
    fun foo(): String = ""
    val a: String
        get() = ""
    fun baz(): String = ""
}

@MustUseReturnValue
class Derived : Base

@MustUseReturnValue
class DerivedWithOverride : Base {
    override val a: String
        get() = ""

    override fun foo() = "Derived"

    @IgnorableReturnValue
    override fun baz() = "Derived"
}

@MustUseReturnValue
interface BaseWithAnnotation {
    fun foo(): String = ""
    val a : String
        get() = ""
    fun baz(): String = ""
}

class DerivedFromAnnotated : BaseWithAnnotation

class DerivedFromAnnotatedWithOverride : BaseWithAnnotation {
    override fun foo() = "DerivedFromAnnotated"

    override val a: String
        get() = "DerivedFromAnnotated"

    @IgnorableReturnValue
    override fun baz() = "DerivedFromAnnotated"
}

fun usage() {
    Derived().foo()
    Derived().a
    DerivedWithOverride().<!RETURN_VALUE_NOT_USED!>foo<!>()
    DerivedWithOverride().<!RETURN_VALUE_NOT_USED!>a<!>
    DerivedWithOverride().baz()
    DerivedFromAnnotated().<!RETURN_VALUE_NOT_USED!>foo<!>()
    DerivedFromAnnotated().<!RETURN_VALUE_NOT_USED!>a<!>
    DerivedFromAnnotatedWithOverride().<!RETURN_VALUE_NOT_USED!>foo<!>()
    DerivedFromAnnotatedWithOverride().<!RETURN_VALUE_NOT_USED!>a<!>
    DerivedFromAnnotatedWithOverride().baz()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, interfaceDeclaration, override,
propertyDeclaration, stringLiteral */
