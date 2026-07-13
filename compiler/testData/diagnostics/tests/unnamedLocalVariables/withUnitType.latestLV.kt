// ISSUE: KT-84618
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +UnnamedLocalVariables +NameBasedDestructuring
// LATEST_LV_DIFFERENCE

// FILE: JavaUtils.java

package test;

public class JavaUtils {
    public static <T> T id(T arg) {
        return arg;
    }

    public static void primitiveVoid() {
    }

    public static Void nonPrimitiveVoid() {
        return null;
    }

    @org.jetbrains.annotations.Nullable
    public static Void nullableVoid() {
        return null;
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
    val <!UNNAMED_PROPERTY_WITH_IMPLICIT_IGNORABLE_TYPE("Unit")!>_<!> = Unit
    val <!UNNAMED_PROPERTY_WITH_IMPLICIT_IGNORABLE_TYPE!>_<!> = returnUnit()
    val _ = returnNullableUnit()
    val <!UNNAMED_PROPERTY_WITH_IMPLICIT_IGNORABLE_TYPE!>_<!> = returnUnitAlias()
    val <!UNNAMED_PROPERTY_WITH_IMPLICIT_IGNORABLE_TYPE!>_<!> = JavaUtils.id(Unit)
    val <!UNNAMED_PROPERTY_WITH_IMPLICIT_IGNORABLE_TYPE!>_<!> = JavaUtils.primitiveVoid()
    val <!UNNAMED_PROPERTY_WITH_IMPLICIT_IGNORABLE_TYPE("Void")!>_<!> = JavaUtils.nonPrimitiveVoid()
    val _ = JavaUtils.nullableVoid()

    val [_, _] = MyPair()
    [val _, val _] = MyPair()

    for (<!UNNAMED_PROPERTY_WITH_IMPLICIT_IGNORABLE_TYPE!>_<!> in arrayOf(Unit, Unit, Unit)) {
    }

    when (val <!UNNAMED_PROPERTY_WITH_IMPLICIT_IGNORABLE_TYPE!>_<!> = returnUnit()) {
        Unit -> {}
    }

    arrayOf(Unit).myForEach { _ -> }
    arrayOf(MyPair()).myForEach { (<!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>, <!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>) -> }
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
    val _: Unit = JavaUtils.primitiveVoid()
    val _: Void = JavaUtils.nonPrimitiveVoid()
    val _: Unit? = JavaUtils.primitiveVoid()
    val _: Void? = JavaUtils.nonPrimitiveVoid()
    val _: Void? = JavaUtils.nullableVoid()

    val [_: Unit, _: Unit?] = MyPair()
    [val _: Unit, val _: Unit?] = MyPair()

    for (_: Unit in arrayOf(Unit, Unit, Unit)) {
    }

    when (val _: Unit = returnUnit()) {
        Unit -> {}
    }

    arrayOf(Unit).myForEach { _: Unit -> }
    arrayOf(MyPair()).myForEach { (<!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>: Unit, <!NAME_BASED_DESTRUCTURING_UNDERSCORE_WITHOUT_RENAMING!>_<!>: Unit?) -> }
    arrayOf(MyPair()).myForEach { [_: Unit, _: Unit?] -> }
    arrayOf(Unit).myForEachIndexed { _: Int, _: Unit -> }
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, equalityExpression, forLoop, functionDeclaration,
localProperty, nullableType, operator, propertyDeclaration, typeAliasDeclaration, unnamedLocalVariable, whenExpression,
whenWithSubject */
