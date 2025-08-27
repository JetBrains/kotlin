// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <T1> of(vararg vals: T1): MyList<T1> = MyList<T1>()
    }
}

fun acceptList(s: String, l: MyList<String>) = Unit
fun acceptList(i: Int, l: MyList<Int>) = Unit

fun test() {
    acceptList(0, [1, 2, 3])
    acceptList("0", ["1", "2", "3"])
    acceptList(42, [])
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, nullableType,
objectDeclaration, operator, stringLiteral, typeParameter, vararg */
