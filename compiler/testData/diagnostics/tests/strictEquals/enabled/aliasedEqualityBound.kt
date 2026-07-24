// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

// FILE: throughTypeAlias.kt

typealias MyEqualityBound = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_ERROR!>EqualityBound<!>

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
