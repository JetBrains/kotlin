import java.util.Arrays

class MyCollection<T>(val delegate: Collection<T>): Collection<T> by delegate

fun box(): String {
    val collection = MyCollection(Arrays.asList(2, 3, 9)) as java.util.Collection<*>

    val array1 = collection.toArray()
    val array2 = collection.toArray(arrayOfNulls<Int>(3) as Array<Int>)

    if (array1 !is Array<Any>) return (array1 as Object).getClass().toString()
    if (array2 !is Array<Int>) return (array2 as Object).getClass().toString()

    val s1 = Arrays.toString(array1)
    val s2 = Arrays.toString(array2)

    if (s1 != "[2, 3, 9]") return "s1 = $s1"
    if (s2 != "[2, 3, 9]") return "s2 = $s2"

    return "OK"
}