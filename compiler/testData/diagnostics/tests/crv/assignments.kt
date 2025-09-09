// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValues

fun stringF(): String = ""

@MustUseReturnValues
class MyList<T> {
    operator fun set(index: Int, value: T): T {
        return value
    }

    operator fun get(index: Int): T {
        TODO()
    }
}

@MustUseReturnValues
class MyMap<K, V> {
    operator fun set(key: K, value: V): V? {
        return null
    }

    operator fun get(key: K): V {
        TODO()
    }
}

fun lhs(map: MutableMap<String, String>, map2: MyMap<String, String>) {
    map["a"] = stringF()
    map[stringF()] = "a"
    map["a"]
    map2["a"] = stringF() // always ignore operator form
    map2.set("a", stringF()) // report regular form
    map2["a"]
}

fun nested(map: List<MutableMap<String, String>>, l2: MyList<MyMap<String, String>>) {
    map[0]["b"] = stringF()
    map[0][stringF()] = stringF()
    map[0]["b"]
    l2[0]["b"] = stringF()
    l2[0] = MyMap()
    l2[0].set(stringF(), "a")
    l2.set(1, MyMap())
    l2[0]["b"]
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, assignment, classDeclaration, functionDeclaration, integerLiteral,
nullableType, operator, stringLiteral, typeParameter */
