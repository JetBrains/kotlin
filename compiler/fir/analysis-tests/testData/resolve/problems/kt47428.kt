// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-47428

// KT-47428: Unexpected "deimport" of nested class type parameter upper bound with star projections involved

open class A<K : Any>

class G : A<G.Key<*>>() {
    class Key<T : Any>
}

object Context {
    interface Slice<K : Any, V : Any>

    object Slices {
        object GENERAL : Slice<GENERAL.Key<*>, Any> {
            interface Key<T : Any>
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nestedClass, objectDeclaration, starProjection,
typeConstraint, typeParameter */
