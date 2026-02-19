// RUN_PIPELINE_TILL: BACKEND
/*
    Here element has type Captured(*) because of resolution for `iterator()` on this.
    and where we resolve `destination.add(element)` we approximate element to `Any` with smart cast to `R`.
 */
inline fun <reified R, C : MutableCollection<in R>> Array<*>.filterIsInstanceTo(destination: C): C {
    for (element in this) if (element is R) destination.add(element)
    return destination
}

/* GENERATED_FIR_TAGS: capturedType, forLoop, funWithExtensionReceiver, functionDeclaration, ifExpression, inProjection,
inline, isExpression, localProperty, nullableType, propertyDeclaration, reified, smartcast, starProjection,
thisExpression, typeConstraint, typeParameter */
