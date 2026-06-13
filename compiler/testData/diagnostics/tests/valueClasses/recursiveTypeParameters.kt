// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-85848, KT-84904
// WITH_STDLIB
// LANGUAGE: +JvmInlineMultiFieldValueClasses, +ForbidValueClassRecursionViaTypeParameters

@JvmInline
value class TestRecursionInUpperBounds1<T : TestRecursionInUpperBounds1<T>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T<!>)
@JvmInline
value class TestRecursionInUpperBounds2<T : TestRecursionInUpperBounds2<T>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T?<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T?<!>)
@JvmInline
value class TestRecursionInUpperBounds2_1<T : TestRecursionInUpperBounds2_1<T>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T?<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T<!>)
@JvmInline
value class TestRecursionInUpperBounds2_2<T : TestRecursionInUpperBounds2_2<T>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T?<!>)
@JvmInline
value class TestRecursionInUpperBounds3<T : TestRecursionInUpperBounds3<T>?>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>T<!>)

@JvmInline
value class V1<T : S, S : R, R : V1<T, S, R>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R<!>)
@JvmInline
value class V2<T : S, S : R, R : V2<T, S, R>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R?<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R?<!>)
@JvmInline
value class V2_1<T : S, S : R, R : V2_1<T, S, R>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R?<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R<!>)
@JvmInline
value class V2_2<T : S, S : R, R : V2_2<T, S, R>>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R?<!>)
@JvmInline
value class V3<T : S, S : R, R : V3<T, S, R>?>(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_ERROR!>R<!>)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, typeConstraint,
typeParameter */
