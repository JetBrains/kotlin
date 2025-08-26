// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        operator fun <T> of(vararg vals: T): MyList = MyList()
    }
}

fun acceptList(l: MyList) = Unit

class A

fun test() {
    acceptList(MyList.of("1", "2", "3"))
    acceptList(MyList.of("0", A()))
    acceptList(["1", "2", "3"])
    acceptList(["0", A()])
    acceptList(["0", null])
    acceptList(["0", A(), null])
    acceptList(MyList.of<String>())
    acceptList(MyList.of<<!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING!>Nothing<!>>())
    acceptList(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>) // should not pass
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, typeParameter, vararg */
