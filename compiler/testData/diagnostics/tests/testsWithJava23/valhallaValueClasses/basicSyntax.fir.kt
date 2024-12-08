// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// RUN_PIPELINE_TILL: FRONTEND
// JVM_TARGET: 23

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

<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>value<!> class B<T>(val x: A, val y: Int = 0, val z: T, <!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>var t: Int<!>, <!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!><!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val s: Long<!>): <!VALUE_CLASS_CANNOT_EXTEND_CLASSES!>Abstract<!>(), Cloneable {
    inner <!VALUE_CLASS_NOT_TOP_LEVEL!>value<!> class Inner(val x: Int)
    inner class Inner1(val x: Int)
    init {
        <!VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>value<!> class Local(val x: Long)
    }
}

<!VALUE_CLASS_NOT_FINAL!>abstract<!> <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class C

<!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>value<!> class D {
    constructor(x: Int)
}

value class NothingWrapper(val x: Nothing)
value class UnitWrapper(val x: Unit)

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
