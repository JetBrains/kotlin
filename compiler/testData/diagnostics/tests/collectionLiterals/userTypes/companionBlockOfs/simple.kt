// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

class MyList<T> {
    companion {
        operator fun <T> of(vararg x: T): MyList<T> = MyList()
    }
}

fun Int.expectedMyListInt(): MyList<Int> {
    when (this) {
        0 -> {
            return [1, 2, 3]
        }
        1 -> {
            return []
        }
        else -> {
            return [<!ARGUMENT_TYPE_MISMATCH!>"!"<!>]
        }
    }
}

fun test() {
    fun <T> accept(x: MyList<T>) { }

    <!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept([1, 2, 3])
    accept(["!"])
    accept([1L, 2, 3.toByte()])

    accept<String>([])
    accept<String>([<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>])
    accept<String>(["!"])

    val nullable: MyList<*>? = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, funWithExtensionReceiver, functionDeclaration,
integerLiteral, localFunction, localProperty, nullableType, operator, propertyDeclaration, starProjection, stringLiteral,
typeParameter, vararg, whenExpression, whenWithSubject */
