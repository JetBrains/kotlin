// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        operator fun of(vararg args: String): MyList = MyList()
    }
}

class Foo

annotation class MyListAnno(val arg: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>MyList<!>)
@Repeatable
annotation class IntArrayAnno(val arg: IntArray = [])
@Repeatable
annotation class StringArrayAnno(val arg: Array<String>)
annotation class FooArrayAnno(val arg: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<Foo><!>)
@Repeatable
annotation class ArrayArrayAnno(val arg: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<Array<String>><!> = [[]])
annotation class BadDefaultParametert(val arg: Array<String> <!INITIALIZER_TYPE_MISMATCH!>=<!> [[]])

@MyListAnno(<!ARGUMENT_TYPE_MISMATCH!>["1", "2", "3"]<!>)
@IntArrayAnno([1, 2, 3])
@IntArrayAnno
@IntArrayAnno(intArrayOf(1, 2, 3))
@StringArrayAnno(["1", "2", "3"])
@StringArrayAnno([])
<!NO_VALUE_FOR_PARAMETER!>@StringArrayAnno<!>
@StringArrayAnno(<!ARGUMENT_TYPE_MISMATCH!>[[]]<!>)
@StringArrayAnno(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>)
@StringArrayAnno(arrayOf())
@StringArrayAnno(arrayOf("1", "2", "3"))
@StringArrayAnno(<!ARGUMENT_TYPE_MISMATCH!>arrayOf(arrayOf())<!>)
@FooArrayAnno(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Foo()<!>]<!>)
@ArrayArrayAnno([[""]])
@ArrayArrayAnno([])
@ArrayArrayAnno(<!ARGUMENT_TYPE_MISMATCH!>[[[]]]<!>)
fun target() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, companionObject, functionDeclaration,
integerLiteral, objectDeclaration, operator, primaryConstructor, propertyDeclaration, stringLiteral, vararg */
