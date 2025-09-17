// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// WITH_REFLECT

import kotlin.reflect.full.primaryConstructor

enum class MyList {
    ELEM;
    companion object {
        operator fun of(vararg i: String) = MyList.ELEM
    }
}

class Foo

annotation class AnnoFoo

annotation class MyListAnno(val arg: MyList)
annotation class IntArrayAnno(val arg: IntArray = [])
annotation class StringArrayAnno(val arg: Array<String>)
annotation class FooArrayAnno(val arg: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<Foo><!>)
annotation class AnnoFooArrayAnno(val arg: Array<AnnoFoo>)

fun test(foo: Foo, annoFoo: AnnoFoo) {
    MyListAnno([])
    MyListAnno(["1", "2", "3"])
    MyListAnno(<!ARGUMENT_TYPE_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>()<!>)
    MyListAnno(<!ARGUMENT_TYPE_MISMATCH!>arrayOf<String>()<!>)

    IntArrayAnno(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    IntArrayAnno(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    IntArrayAnno(<!ARGUMENT_TYPE_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>()<!>)
    IntArrayAnno(intArrayOf())
    IntArrayAnno(run {
        <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
    })
    IntArrayAnno(run {
        val x = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
        x
    })
    IntArrayAnno(run {
        val x: IntArray = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
        x
    })

    StringArrayAnno(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    StringArrayAnno(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>)
    StringArrayAnno(arrayOf())
    StringArrayAnno(arrayOf<String>())
    StringArrayAnno(run {
        <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>
    })
    StringArrayAnno(run {
        <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    })
    StringArrayAnno(run {
        val x = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>
        x
    })
    StringArrayAnno::class.primaryConstructor?.call(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    StringArrayAnno::class.primaryConstructor?.call(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>)

    FooArrayAnno(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    FooArrayAnno(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[Foo()]<!>)
    FooArrayAnno(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[foo]<!>)

    AnnoFooArrayAnno(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    AnnoFooArrayAnno(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[AnnoFoo()]<!>)
    AnnoFooArrayAnno(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[annoFoo]<!>)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, collectionLiteral, companionObject,
enumDeclaration, functionDeclaration, integerLiteral, lambdaLiteral, localProperty, nullableType, objectDeclaration,
operator, primaryConstructor, propertyDeclaration, safeCall, stringLiteral, vararg */
