// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND

interface Box<T> {
    var x: T
}

fun <Z: Set<Int>> buildBox(block: Box<Z>.() -> Unit): Box<Z> = TODO()
fun <Z: MutableCollection<Int>> buildBox2(block: Box<Z>.() -> Unit): Box<Z> = TODO()

fun test() {
    buildBox {
        x = []
    }

    buildBox {
        x = [<!ARGUMENT_TYPE_MISMATCH!>"!"<!>]
    }

    buildBox {
        x = [42]
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildBox2<!> {
        x = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildBox2<!> {
        x = [42]
    }

    buildBox2 {
        x = mutableSetOf()
        x = []
    }

    buildBox2 {
        x = mutableSetOf()
        x = [<!ARGUMENT_TYPE_MISMATCH!>"!"<!>]
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, propertyDeclaration, stringLiteral, typeConstraint, typeParameter, typeWithExtension */
