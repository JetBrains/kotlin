// !WITH_NEW_INFERENCE
//KT-1127 Wrong type computed for Arrays.asList()

package d

fun <T> asList(t: T) : List<T>? {}

fun main() {
    val list : List<String> = asList("")
}
