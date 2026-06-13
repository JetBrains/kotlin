// RUN_PIPELINE_TILL: BACKEND
package a.b

class C<T, out S> {
    inner class D<R, in P> {

    }
}

interface Test {
    val x: a.b.C<out CharSequence, *>.D<in List<*>, *>
}

/* GENERATED_FIR_TAGS: classDeclaration, in, inProjection, inner, interfaceDeclaration, nullableType, out, outProjection,
propertyDeclaration, starProjection, typeParameter */
