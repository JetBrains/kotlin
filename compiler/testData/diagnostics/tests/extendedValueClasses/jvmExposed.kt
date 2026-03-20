// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +ValueClasses


@file:OptIn(ExperimentalStdlibApi::class)

value class PositiveInt1 <!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!> constructor(val value: Int) {
    <!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!> fun toInt(): Int = value
}

abstract value class PositiveInt2 <!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!> constructor() {
    abstract val value: Int
    <!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!> fun toInt(): Int = value
}

sealed value class PositiveInt3 <!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!> constructor() {
    abstract val value: Int
    <!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!> fun toInt(): Int = value
}

<!VALUE_CLASS_OPEN!>open<!> value class PositiveInt4 <!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!> constructor() {
    <!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!> fun toInt(): Int = 3
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, functionDeclaration,
primaryConstructor, propertyDeclaration, value */
