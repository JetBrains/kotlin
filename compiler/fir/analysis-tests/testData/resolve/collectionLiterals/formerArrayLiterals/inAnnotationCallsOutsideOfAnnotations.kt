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

    IntArrayAnno([])
    IntArrayAnno([1, 2, 3])
    IntArrayAnno(<!ARGUMENT_TYPE_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE!>arrayOf<!>()<!>)
    IntArrayAnno(intArrayOf())
    IntArrayAnno(run {
        [1, 2, 3]
    })
    IntArrayAnno(<!ARGUMENT_TYPE_MISMATCH!>run {
        val x = [1, 2, 3]
        x
    }<!>)
    IntArrayAnno(run {
        val x: IntArray = [1, 2, 3]
        x
    })

    StringArrayAnno([])
    StringArrayAnno(["1", "2", "3"])
    StringArrayAnno(arrayOf())
    StringArrayAnno(arrayOf<String>())
    StringArrayAnno(run {
        ["1", "2", "3"]
    })
    StringArrayAnno(run {
        []
    })
    StringArrayAnno(<!ARGUMENT_TYPE_MISMATCH!>run {
        val x = ["1", "2", "3"]
        x
    }<!>)
    StringArrayAnno::class.primaryConstructor?.call(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    StringArrayAnno::class.primaryConstructor?.call(["1", "2", "3"])

    FooArrayAnno([])
    FooArrayAnno([Foo()])
    FooArrayAnno([foo])

    AnnoFooArrayAnno([])
    AnnoFooArrayAnno([AnnoFoo()])
    AnnoFooArrayAnno([annoFoo])
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, collectionLiteral, companionObject,
enumDeclaration, functionDeclaration, integerLiteral, lambdaLiteral, localProperty, nullableType, objectDeclaration,
operator, primaryConstructor, propertyDeclaration, safeCall, stringLiteral, vararg */
