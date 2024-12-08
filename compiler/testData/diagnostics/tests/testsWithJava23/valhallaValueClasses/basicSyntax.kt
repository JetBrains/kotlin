// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// RUN_PIPELINE_TILL: FRONTEND
// JVM_TARGET: 23

<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class A(val x: Int): Comparable<Int> by x {
    init {
        println("$x + $x")
    }
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val y<!> = x
    val z get() = x
    val t <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by lazy { x }<!>
    
    <!INNER_CLASS_INSIDE_VALUE_CLASS!>inner<!> class Inner;
    
    constructor(l: Long): this(l.toInt()) {
        println("Secondary!")
    }
}

abstract class Abstract {
    inner <!VALUE_CLASS_NOT_TOP_LEVEL, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class Inner(val x: Int)
}

<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class B<T><!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: A, val y: Int = 0, val z: T, var t: Int, <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val s: Long)<!>: Abstract(), Cloneable {
    <!INNER_CLASS_INSIDE_VALUE_CLASS!>inner<!> <!VALUE_CLASS_NOT_TOP_LEVEL, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class Inner(val x: Int)
    <!INNER_CLASS_INSIDE_VALUE_CLASS!>inner<!> class Inner1(val x: Int)
    init {
        <!VALUE_CLASS_NOT_TOP_LEVEL, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION, WRONG_MODIFIER_TARGET!>value<!> class Local(val x: Long)
    }
}

<!VALUE_CLASS_NOT_FINAL!>abstract<!> <!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class C

<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class D {
    constructor(x: Int)
}

<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class NothingWrapper(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Nothing<!>)
<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class UnitWrapper(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)

<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline1(val x: <!VALUE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE!>Unit<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline2<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Unit, val y: Unit)<!>


@JvmInline
value class OldSingleFieldValueClass(val x: Int)

@JvmInline
value class OldMultiFieldValueClass<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: Int)<!>

fun main() {
    A(2).<!UNRESOLVED_REFERENCE!>copy<!>()
    val (x) = <!COMPONENT_FUNCTION_MISSING!>A(2)<!>
}
