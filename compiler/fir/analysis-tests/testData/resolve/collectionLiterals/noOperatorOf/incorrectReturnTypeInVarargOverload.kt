// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

sealed class MyList {
    class MyListImpl : MyList()
    companion object {
        operator fun of(vararg strs: String): MyListImpl = MyListImpl()
    }
}

open class MyGenericList<T> {
    companion object {
        operator fun <T1> of(vararg t: T1): MyGenericListImpl<T1> = MyGenericListImpl<T1>()
    }
}

class MyGenericListImpl<T3>: MyGenericList<T3>()

interface MyGenericInterfaceWithNonGenericImpl<U> {
    companion object {
        operator fun of(vararg s: String): MyGenericInterfaceImpl = MyGenericInterfaceImpl()
    }
}

class MyGenericInterfaceImpl : MyGenericInterfaceWithNonGenericImpl<String>

fun acceptList(l: MyList) { }
fun <T2> acceptGenericList(l: MyGenericList<T2>) { }
fun acceptGenericInterfaceString(l: MyGenericInterfaceWithNonGenericImpl<String>) { }
fun <U2> acceptGenericInterface(l: MyGenericInterfaceWithNonGenericImpl<U2>) { }

fun test() {
    acceptList(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGenericList<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>)
    acceptGenericInterfaceString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGenericInterface<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, interfaceDeclaration, nestedClass,
nullableType, objectDeclaration, operator, sealed, stringLiteral, typeParameter, vararg */
