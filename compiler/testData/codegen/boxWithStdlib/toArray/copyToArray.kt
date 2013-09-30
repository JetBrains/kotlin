import java.util.Arrays

fun box(): String {
    val array = Arrays.asList(2, 3, 9).copyToArray()
    if (array !is Array<Int>) return array.javaClass.toString()

    val str = Arrays.toString(array)
    if (str != "[2, 3, 9]") return str

    return "OK"
}