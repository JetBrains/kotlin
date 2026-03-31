// RUN_PIPELINE_TILL: FRONTEND
// DUMP_INFERENCE_LOGS: FIXATION, MARKDOWN
// ISSUE: KT-85405

abstract class Shape

interface SmithyBuilder<T> {
    fun build(): T
}
interface ToSmithyBuilder<T> {
    fun toBuilder(): SmithyBuilder<T>
}
abstract class AbstractShapeBuilder<B : AbstractShapeBuilder<B, S>, S : Shape> : SmithyBuilder<S> {
    @Suppress("UNCHECKED_CAST")
    fun removeTrait(/*...*/): B = /* ... */ this as B
}

abstract class OperationShape : Shape(), ToSmithyBuilder<OperationShape>

fun <T, B> T.removeTraitIfPresent(/*...*/): T
        where T : Shape,
              T : ToSmithyBuilder<T>,
              B : AbstractShapeBuilder<B, T>,
              B : SmithyBuilder<T> {
    /* ... */
    @Suppress("UNCHECKED_CAST")
    return (this.toBuilder() as B).removeTrait().build()
}

fun test1(shape: OperationShape) {
    shape.<!UPPER_BOUND_VIOLATED!>removeTraitIfPresent<!>()
}

fun test2(shape: OperationShape) {
    // The type the diagnostic in `test1` suggests
    shape.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>removeTraitIfPresent<!><OperationShape, AbstractShapeBuilder<SmithyBuilder<OperationShape>, OperationShape>>()
}

fun test3(shape: OperationShape) {
    // Proof it's a materialize-like function
    shape.removeTraitIfPresent<OperationShape, Nothing>()
}

fun test4(shape: OperationShape) {
    // A type that "feels right"
    shape.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>removeTraitIfPresent<!><OperationShape, AbstractShapeBuilder<*, OperationShape>>()
}

@Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")
fun <B : AbstractShapeBuilder<B, S>, S : Shape> shapeToBuilder(shape: S): B =
    // In the actual code, there's a call to a Java function returning the raw type AbstractShapeBuilder
    null as B

abstract class SimpleShape : Shape()

fun testA(target: SimpleShape) {
    val builder: AbstractShapeBuilder<*, *> = <!UPPER_BOUND_VIOLATED!>shapeToBuilder<!>(target)
}

fun testB(target: SimpleShape) {
    // The type the diagnostic in `testA` suggests
    val builder: AbstractShapeBuilder<*, *> = <!INAPPLICABLE_CANDIDATE!>shapeToBuilder<!><<!UPPER_BOUND_VIOLATED!>AbstractShapeBuilder<<!UPPER_BOUND_VIOLATED!>AbstractShapeBuilder<<!UPPER_BOUND_VIOLATED!>out AbstractShapeBuilder<*, *><!>, SimpleShape><!>, SimpleShape><!>, SimpleShape>(target)
}

fun testC(target: SimpleShape) {
    // A type that "feels right"
    val builder: AbstractShapeBuilder<*, *> = <!INAPPLICABLE_CANDIDATE!>shapeToBuilder<!><<!UPPER_BOUND_VIOLATED!>AbstractShapeBuilder<*, SimpleShape><!>, SimpleShape>(target)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, localProperty, primaryConstructor,
propertyDeclaration */
