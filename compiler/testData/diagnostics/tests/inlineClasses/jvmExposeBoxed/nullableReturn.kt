// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val s: String)

// A nullable value class is already boxed in the JVM signature, so the unboxed variant is not mangled and the
// boxed one would collide with it. At top level that leaves no way to tell them apart without a name.
<!JVM_EXPOSE_BOXED_REQUIRES_NAME!>@JvmExposeBoxed<!>
fun nullableReturn(): IC? = TODO()

@JvmExposeBoxed("namedNullableReturn")
fun nullableReturnNamed(): IC? = TODO()

<!JVM_EXPOSE_BOXED_REQUIRES_NAME!>@get:JvmExposeBoxed<!>
val nullableGetter: IC? = TODO()

class C {
    // Members are mangled by return type, so a member needs no name - the same must hold for a nullable one.
    @JvmExposeBoxed
    fun nullableReturn(): IC? = TODO()
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, annotationUseSiteTargetPropertyGetter, classDeclaration,
classReference, functionDeclaration, nullableType, primaryConstructor, propertyDeclaration, stringLiteral, value */
