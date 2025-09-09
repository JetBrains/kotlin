// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

interface Base {
    fun foo(): String = ""
    val a: String
        get() = ""
    fun baz(): String = ""
}

@MustUseReturnValues
class Derived : Base

@MustUseReturnValues
class DerivedWithOverride : Base {
    override val a: String
        get() = ""

    override fun foo() = "Derived"

    @IgnorableReturnValue
    override fun baz() = "Derived"
}

@MustUseReturnValues
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
    DerivedWithOverride().foo()
    DerivedWithOverride().a
    DerivedWithOverride().baz()
    DerivedFromAnnotated().foo()
    DerivedFromAnnotated().a
    DerivedFromAnnotatedWithOverride().foo()
    DerivedFromAnnotatedWithOverride().a
    DerivedFromAnnotatedWithOverride().baz()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, interfaceDeclaration, override,
propertyDeclaration, stringLiteral */
