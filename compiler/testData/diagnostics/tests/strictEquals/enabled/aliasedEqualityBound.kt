// RUN_PIPELINE_TILL: BACKEND

// FILE: throughTypeAlias.kt

typealias MyEqualityBound = EqualityBound

class Foo {
    override fun equals(@MyEqualityBound(Foo::class) other: Any?): Boolean = true
}

// FILE: throughImport.kt

import kotlin.EqualityBound as MyEqualityBound

class Bar {
    override fun equals(@MyEqualityBound(Bar::class) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, operator, override,
typeAliasDeclaration */
