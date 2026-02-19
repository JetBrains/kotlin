// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

open class MyList<T> {
    companion object {
        operator fun <T> of(vararg t: T): MyList<T> = MyList()
    }
}

fun <X> id(x: X): X = x

fun <Y : MyList<*>> restrictedId(y: Y): Y = y

fun <Z : MyList<String>> fullyRestrictedId(z: Z): Z = z

fun test() {
    id<MyList<String>>([])
    id<MyList<String>>(["42"])
    id<MyList<String>>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)

    id<MyList<MyList<String>>>([])
    id<MyList<MyList<String>>>([[]])
    id<MyList<MyList<String>>>([["42"]])
    id<MyList<MyList<String>>>([<!ARGUMENT_TYPE_MISMATCH!>[null]<!>])

    id<MyList<*>>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    id<MyList<*>>(["42"])
    id<MyList<*>>([null])

    <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>["42"]<!>)

    id<MyList<MyList<*>>>([])
    id<MyList<MyList<*>>>([<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>])
    id<MyList<MyList<*>>>([["42"]])

    restrictedId<MyList<String>>([])
    restrictedId<MyList<String>>(["42"])
    restrictedId<MyList<String>>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)

    restrictedId<MyList<*>>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    restrictedId<MyList<*>>(["42"])
    restrictedId<MyList<*>>([null])

    <!CANNOT_INFER_PARAMETER_TYPE!>restrictedId<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    restrictedId(["42"])

    fullyRestrictedId([])
    fullyRestrictedId(["42"])
    <!CANNOT_INFER_PARAMETER_TYPE!>fullyRestrictedId<!>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, nullableType,
objectDeclaration, operator, starProjection, stringLiteral, typeConstraint, typeParameter, vararg */
