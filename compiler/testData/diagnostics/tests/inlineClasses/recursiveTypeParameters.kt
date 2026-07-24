// LANGUAGE: +ForbidValueClassRecursionViaTypeParameters
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-85848, KT-84904
// WITH_STDLIB

@JvmInline
value class TestRecursionInUpperBounds1<T : TestRecursionInUpperBounds1<T>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T<!>)
@JvmInline
value class TestRecursionInUpperBounds2<T : TestRecursionInUpperBounds2<T>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T?<!>)
@JvmInline
value class TestRecursionInUpperBounds3<T : TestRecursionInUpperBounds3<T>?>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T<!>)

@JvmInline
value class V1<T : S, S : R, R : V1<T, S, R>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R<!>)
@JvmInline
value class V2<T : S, S : R, R : V2<T, S, R>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R?<!>)
@JvmInline
value class V3<T : S, S : R, R : V3<T, S, R>?>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R<!>)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, typeConstraint,
typeParameter */
