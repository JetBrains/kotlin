// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78069
// LANGUAGE: +DataFlowBasedExhaustiveness

enum class MyEnum { A, B, C }

sealed interface MySealedInterface {
    object A : MySealedInterface
    object B : MySealedInterface
    object C : MySealedInterface
}

fun <T> genericEnum(e: T): Int where T : MyEnum {
    if (e == MyEnum.A) return 1
    return when (e) {
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

fun <T> genericEnumWithLocal(e: T): Int where T : MyEnum {
    val y: MyEnum = e
    if (y == MyEnum.A) return 1
    return <!NO_ELSE_IN_WHEN!>when<!> (e) { // KT-78069
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

fun <T> genericSealed(x: T): Int where T : MySealedInterface {
    if (x is MySealedInterface.A) return 1
    return when (x) {
        MySealedInterface.B -> 2
        MySealedInterface.C -> 3
    }
}

fun <T> genericEnumThrow(e: T): Int where T : MyEnum {
    (e == MyEnum.A) && throw AssertionError("A")

    return when (e) {
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

fun <T> nestedBounded(u: T): Int where T : MySealedInterface, T : java.io.Serializable {
    val sealedValue: MySealedInterface = u

    if (sealedValue is MySealedInterface.C) return 1

    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (sealedValue) {
        MySealedInterface.A -> 2
        MySealedInterface.B -> 3
    }<!>
}

fun <T> genericEnumNullable(e: T?): Int where T : MyEnum {
    if (e == null) return 0
    if (e == MyEnum.A) return 1

    return when (e) {
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

fun <T> genericEnumMulti(e: T): Int where T : MyEnum {
    if (e == MyEnum.C) return 1

    return when (e) {
        MyEnum.A, MyEnum.B -> 2
    }
}

fun <T> genericEnumLocalMulti(e: T): Int where T : MyEnum {
    val y: MyEnum = e
    if (y == MyEnum.B || y == MyEnum.C) return 1

    return <!NO_ELSE_IN_WHEN!>when<!> (e) {
        MyEnum.A -> 2
    }
}

/* GENERATED_FIR_TAGS: andExpression, disjunctionExpression, dnnType, enumDeclaration, enumEntry, equalityExpression,
functionDeclaration, ifExpression, integerLiteral, interfaceDeclaration, isExpression, localProperty, nestedClass,
nullableType, objectDeclaration, propertyDeclaration, sealed, smartcast, stringLiteral, typeConstraint, typeParameter,
whenExpression, whenWithSubject */
