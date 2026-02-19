// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS
// DUMP_INFERENCE_LOGS: FIXATION, MARKDOWN

open class Expression<K>

class ModOp<T : Number?, S : Number?>(
    val expr1: Expression<T>,
    val expr2: Expression<S>
)

class QueryParameter<A> : Expression<A>()

fun <K, R : K?> Expression<in R>.wrap(value: K): QueryParameter<K> = null as QueryParameter<K>

fun <M : Number?, Z : M> Expression<M>.rem(t: Z): ModOp<M, Z> = ModOp(this, wrap(t))

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, inProjection,
intersectionType, nullableType, primaryConstructor, propertyDeclaration, thisExpression, typeConstraint, typeParameter */
