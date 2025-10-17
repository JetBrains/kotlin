// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        operator fun of(vararg xs: Int): MyTypealias = MyList()
    }
}

typealias MyTypealias = MyList

class MyGenericList<T> {
    companion object {
        operator fun of(vararg xs: Int): MyGenericTypealias = MyGenericList<Int>()
    }
}

typealias MyGenericTypealias = MyGenericList<Int>

fun test() {
    val a: MyList = [1, 2, 3]
    val b: MyTypealias = [1, 2, 3]
    val c: MyGenericList<Int> = [1, 2, 3]
    val d: MyGenericTypealias = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, integerLiteral,
localProperty, nullableType, objectDeclaration, operator, propertyDeclaration, typeAliasDeclaration, typeParameter,
vararg */
