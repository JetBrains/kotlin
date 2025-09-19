// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: -CollectionLiterals

class MyList {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun of(vararg args: String): MyList = MyList()
    }
}

class Foo

annotation class MyListAnno(val arg: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>MyList<!>)
annotation class IntArrayAnno(val arg: IntArray = [])
annotation class StringArrayAnno(val arg: Array<String>)
annotation class FooArrayAnno(val arg: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<Foo><!>)

@MyListAnno(<!ARGUMENT_TYPE_MISMATCH!>["1", "2", "3"]<!>)
@IntArrayAnno([1, 2, 3])
@StringArrayAnno(["1", "2", "3"])
@FooArrayAnno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Foo()<!>]<!>)
fun target() = Unit

@StringArrayAnno([])
@IntArrayAnno
fun secondTarget() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, companionObject, functionDeclaration,
integerLiteral, objectDeclaration, operator, primaryConstructor, propertyDeclaration, stringLiteral, vararg */
