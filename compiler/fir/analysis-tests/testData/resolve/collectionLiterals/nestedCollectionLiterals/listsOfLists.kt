// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <K> of(vararg k: K) = MyList<K>()
    }
}

fun takeListListInt(lst: MyList<MyList<Int>>) { }
fun <U> takeListList(lst: MyList<MyList<U>>) { }

fun test() {
    takeListListInt([])
    takeListListInt([[]])
    takeListListInt([[1, 2, 3]])

    takeListListInt([<!ARGUMENT_TYPE_MISMATCH!>[1, '2', 3]<!>])
    takeListListInt([<!ARGUMENT_TYPE_MISMATCH!>[1, null, 3]<!>])
    takeListListInt([[<!UNRESOLVED_REFERENCE!>[]<!>]])
    takeListListInt(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>)

    takeListList<Int>([])
    takeListList<Int>([[]])
    takeListList<Int>([[1, 2, 3]])

    takeListList<Int>([<!ARGUMENT_TYPE_MISMATCH!>[1, '2', 3]<!>])
    takeListList<Int>([<!ARGUMENT_TYPE_MISMATCH!>[1, null, 3]<!>])
    takeListList<Int>([[<!UNRESOLVED_REFERENCE!>[]<!>]])
    takeListList<Int>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>takeListList<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeListList<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>]<!>)
    takeListList([[1, 2, 3]])
    <!CANNOT_INFER_PARAMETER_TYPE!>takeListList<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!UNRESOLVED_REFERENCE!>[]<!>]<!>]<!>)
    takeListList([[1, '2', 3]])
    takeListList([[1, null, 3]])
    <!CANNOT_INFER_PARAMETER_TYPE!>takeListList<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>)

    var lst: MyList<MyList<Int>> = [[1, 2, 3], <!ARGUMENT_TYPE_MISMATCH!>[4L, 5L, 6L]<!>, [7, 8, 9]]
    lst = [<!ARGUMENT_TYPE_MISMATCH!>["1", "2", "3"]<!>]
    lst = []
    lst = [[]]
    lst = [[<!UNRESOLVED_REFERENCE!>[]<!>]]
    lst = [[<!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>]]
    lst = [[1, 2, 3]]
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, intersectionType,
nullableType, objectDeclaration, operator, typeParameter, vararg */
