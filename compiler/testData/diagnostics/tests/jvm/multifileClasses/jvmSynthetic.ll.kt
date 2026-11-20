// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `NOT_ALL_MULTIFILE_CLASS_PARTS_ARE_JVM_SYNTHETIC`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: LOWERINGS
// WITH_STDLIB
// FILE: f.kt

@file:JvmName("Foo")
@file:JvmMultifileClass
package test

fun f() {}

// FILE: g.kt

@file:JvmName("Foo")
@file:JvmMultifileClass
@file:JvmSynthetic
package test

val g = ""

// FILE: h.kt

@file:JvmName("Foo")
@file:JvmMultifileClass
package test

fun h() {}

// FILE: z.kt

@file:JvmName("Bar")
@file:JvmMultifileClass
package test

fun z() {}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, functionDeclaration, propertyDeclaration, stringLiteral */
