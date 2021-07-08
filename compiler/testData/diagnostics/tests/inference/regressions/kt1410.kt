// FIR_IDENTICAL
// !CHECK_TYPE

// KT-1410 Compiler does automatically infer type argument when using variance
//+JDK
package d

import checkSubtype

public fun <T> MutableCollection<out T>.filterToMy(result : MutableList<in T>, filter : (T) -> Boolean) : MutableCollection<out T> {
    for (t in this){
        if (filter(t)){
            result.add(t)
        }
    }
    return this
}

fun foo(result: MutableList<in String>, collection: MutableCollection<String>, prefix : String){
    collection.filterToMy(result, {it.startsWith(prefix)})
}

fun test(result: MutableList<in Any>, collection: MutableCollection<String>, prefix : String){
    val c = collection.filterToMy(result, {it.startsWith(prefix)})
    checkSubtype<MutableCollection<out String>>(c)
}

//from library
fun String.startsWith(prefix: String) : Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
