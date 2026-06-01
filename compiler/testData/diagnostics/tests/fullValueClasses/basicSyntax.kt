// LANGUAGE: +FullValueClasses, -EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

value class A(val x: Int): Comparable<Int> by x {
    init {
        println("$x + $x")
    }
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val y<!> = x
    val z get() = x
    val t by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>lazy { x }<!>

    inner class Inner;

    constructor(l: Long): this(l.toInt()) {
        println("Secondary!")
    }
}

abstract class Abstract {
    inner <!VALUE_CLASS_NOT_TOP_LEVEL!>value<!> class Inner(val x: Int)
}

<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>value<!> class B<T>(val x: A, val y: Int = 0, val z: T, <!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>var t: Int<!>, <!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val s: Long<!>): <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>Abstract<!>(), Cloneable {
    inner <!VALUE_CLASS_NOT_TOP_LEVEL!>value<!> class Inner(val x: Int)
    inner class Inner1(val x: Int)
    init {
        <!VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>value<!> class Local(val x: Long)
    }
}

abstract value class C
sealed value class C_

<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class D {
    constructor(x: Int)
}

value class NothingWrapper(val x: Nothing)
value class UnitWrapper(val x: Unit)

<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline1(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline2<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Unit, val y: Unit)<!>


@JvmInline
value class BasicSingleFieldValueClass(val x: Int)

@JvmInline
value class BasicMultiFieldValueClass<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: Int)<!>

value class Delegation(val x: Int) : Comparable<Int> by x
value class Delegation1(val x: Int) : <!VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>Comparable<Int><!> by (x.let { 2 + 2 })

@JvmInline
<!VALUE_CLASS_NOT_FINAL("@JvmInline value")!>abstract<!> value class AbstractOld(val x: Int)
@JvmInline
<!VALUE_CLASS_NOT_FINAL!>open<!> value class OpenOld(val x: Int)
@JvmInline
<!VALUE_CLASS_NOT_FINAL!>sealed<!> value class SealedOld(val x: Int)
@JvmInline
value class OverridingOld(val x: Int) : <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>C<!>()

fun main() {
    A(2).<!UNRESOLVED_REFERENCE!>copy<!>()
    val (x) = <!COMPONENT_FUNCTION_MISSING!>A(2)<!>
}


/* GENERATED_FIR_TAGS: classDeclaration, getter, inheritanceDelegation, init, inner, integerLiteral, lambdaLiteral,
localClass, nullableType, primaryConstructor, propertyDeclaration, propertyDelegate, secondaryConstructor,
starProjection, stringLiteral, typeParameter, value */
