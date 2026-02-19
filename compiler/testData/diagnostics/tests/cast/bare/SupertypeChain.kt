// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT
abstract class Parent<K>
abstract class DefaultParent<K, X> : Parent<K>()
abstract class TableDerived<K : A> : DefaultParent<K, Int>() {
    fun bar(): K = TODO()
}

interface A {}
interface B : A { fun b() }

fun foo(): Parent<out B> = TODO()

fun main() {
    val w = foo() as? TableDerived ?: return
    w.bar().b()
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, elvisExpression, functionDeclaration, interfaceDeclaration,
localProperty, nullableType, outProjection, propertyDeclaration, typeConstraint, typeParameter */
