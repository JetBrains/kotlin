// KT-1410 Compiler does automatically infer type argument when using variance
//+JDK
package d

public fun <T> Collection<out T>.filterToMy(result : MutableList<in T>, filter : (T) -> Boolean) : Collection<out T> {
    for (t in this){
        if (filter(t)){
            result.add(t)
        }
    }
    return this
}

fun foo(result: MutableList<in String>, val collection: Collection<String>, prefix : String){
    collection.filterToMy(result, {it.startsWith(prefix)})
}

fun test(result: MutableList<in Any>, val collection: Collection<String>, prefix : String){
    val c = collection.filterToMy(result, {it.startsWith(prefix)})
    c: Collection<out String>
}

//from library
fun String.startsWith(<!UNUSED_PARAMETER!>prefix<!>: String) : Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>