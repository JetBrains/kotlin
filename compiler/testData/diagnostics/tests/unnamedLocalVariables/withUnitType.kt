// ISSUE: KT-84618
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +UnnamedLocalVariables +NameBasedDestructuring

// FILE: JavaUtils.java

package test;

public class JavaUtils {
    public static <T> T id(T arg) {
        return arg;
    }
}

// FILE: test.kt

package test

typealias UnitAlias = Unit

fun returnUnit() { }
fun returnUnitAlias(): UnitAlias { }
fun returnNullableUnit(): Unit? = null

class MyPair {
    operator fun component1() = returnUnit()
    operator fun component2() = returnNullableUnit()
}

inline fun <reified T> Array<T>.myForEach(block: (T) -> Unit) {
    for (element in this) block(element)
}

inline fun <reified T> Array<T>.myForEachIndexed(block: (Int, T) -> Unit) {
    var it = 0
    for (element in this) block(it++, element)
}

fun testWithImplicit() {
    val <!UNNAMED_PROPERTY_WITH_IMPLICIT_UNIT_TYPE!>_<!> = Unit
    val <!UNNAMED_PROPERTY_WITH_IMPLICIT_UNIT_TYPE!>_<!> = returnUnit()
    val _ = returnNullableUnit()
    val <!UNNAMED_PROPERTY_WITH_IMPLICIT_UNIT_TYPE!>_<!> = returnUnitAlias()
    val <!UNNAMED_PROPERTY_WITH_IMPLICIT_UNIT_TYPE!>_<!> = JavaUtils.id(Unit)

    val [_, _] = MyPair()
    [val _, val _] = MyPair()

    for (<!UNNAMED_PROPERTY_WITH_IMPLICIT_UNIT_TYPE!>_<!> in arrayOf(Unit, Unit, Unit)) {
    }

    when (val <!UNNAMED_PROPERTY_WITH_IMPLICIT_UNIT_TYPE!>_<!> = returnUnit()) {
        Unit -> {}
    }

    arrayOf(Unit).myForEach { _ -> }
    arrayOf(MyPair()).myForEach { (_, _) -> }
    arrayOf(Unit).myForEachIndexed { _, _ -> }
}

fun testWithExplicit() {
    val _: Unit = Unit
    val _: Unit = returnUnit()
    val _: Unit? = returnUnit()
    val _: Unit? = returnNullableUnit()
    val _: Unit = JavaUtils.id(Unit)
    val _: Unit? = JavaUtils.id(Unit)
    val _: UnitAlias = Unit
    val _: UnitAlias = returnUnitAlias()

    val [_: Unit, _: Unit?] = MyPair()
    [val _: Unit, val _: Unit?] = MyPair()

    for (_: Unit in arrayOf(Unit, Unit, Unit)) {
    }

    when (val _: Unit = returnUnit()) {
        Unit -> {}
    }

    arrayOf(Unit).myForEach { _: Unit -> }
    arrayOf(MyPair()).myForEach { (_: Unit, _: Unit?) -> }
    arrayOf(Unit).myForEachIndexed { _: Int, _: Unit -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, equalityExpression, forLoop, functionDeclaration,
localProperty, nullableType, operator, propertyDeclaration, typeAliasDeclaration, unnamedLocalVariable, whenExpression,
whenWithSubject */
