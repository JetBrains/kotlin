// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// FILE: internal.kt

data class D(@property:PublishedApi internal val x: Int)

open class Generic<T> {
    @PublishedApi internal val y: T? = null

    @PublishedApi internal fun foo(): T? = null
}

class Derived : Generic<String>()

// FILE: use.kt

inline fun use(data: D, derived: Derived) {
    val (<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>x<!>) = data
    val xx = data.x
    val y = derived.y
    val foo = derived.foo()
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetProperty, classDeclaration, data, destructuringDeclaration,
functionDeclaration, inline, localProperty, primaryConstructor, propertyDeclaration */
