// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +AllowContractsOnPropertyAccessors
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-84960

import kotlin.contracts.*

fun works() {
    val foo: Foo<Unit> = BarImpl
    foo.foo()
    val bar: Bar<Unit> = foo
}

fun fails() {
    val foo: Foo<Unit> = BarImpl
    foo.foo2
    val bar: Bar<Unit> = foo
}

interface Foo<T>
interface Bar<T>: Foo<T>

object BarImpl: Bar<Unit>

fun <T> Foo<T>.foo() {
    contract { returns() implies (this@foo is Bar) }
}

val <T> Foo<T>.foo2: Unit get() {
    contract { returns() implies (this@foo2 is Bar) }
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, funWithExtensionReceiver, functionDeclaration, getter,
interfaceDeclaration, intersectionType, isExpression, lambdaLiteral, localProperty, nullableType, objectDeclaration,
propertyDeclaration, propertyWithExtensionReceiver, smartcast, thisExpression, typeParameter */
