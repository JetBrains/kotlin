// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

interface MyList<U> {
    companion object {
        operator fun <T> of(vararg t: T) = object : MyList<T> { }
    }
}

class Impl1 : MyList<String> by []
class Impl2 : MyList<String> by ["1", "2", "3"]
class Impl3 : MyList<String> by <!TYPE_MISMATCH!>[1, 2, 3]<!>
class Impl4 : MyList<String> by <!TYPE_MISMATCH!>[null]<!>
class Impl5<K> : MyList<String> by ["1", "2", "3"]
class Impl6<K> : MyList<K> by []
class Impl7<K> : MyList<K> by <!TYPE_MISMATCH!>[1, 2, 3]<!>

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, companionObject, functionDeclaration,
inheritanceDelegation, integerLiteral, interfaceDeclaration, nullableType, objectDeclaration, operator, stringLiteral,
typeParameter, vararg */
