// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <T1> of(vararg vals: T1): MyList<T1> = MyList<T1>()
    }
}

class OtherList<K> {
    companion object {
        operator fun <K1> of(vararg vals: K1): OtherList<K1> = OtherList<K1>()
    }
}

fun acceptList(my: MyList<String>) = Unit
fun acceptList(other: OtherList<String>) = Unit

fun test() {
    acceptList(my = ["1", "2", "3"])
    acceptList(other = [])
    acceptList(other = <!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, nullableType,
objectDeclaration, operator, stringLiteral, typeParameter, vararg */
