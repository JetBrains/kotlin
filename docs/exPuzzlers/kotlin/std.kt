namespace std

fun <T> array(vararg array : T) = array
fun array(vararg array : Byte) = array
fun array(vararg array : Char) = array
fun array(vararg array : Short) = array
fun array(vararg array : Int) = array
fun array(vararg array : Long) = array
fun array(vararg array : Double) = array
fun array(vararg array : Float) = array

fun Any?.identityEquals(other : Any?) = this === other

namespace io {
    import java.io.*

    fun print(message : Any?) { System.out?.print(message) }
    fun print(message : Int) { System.out?.print(message) }
    fun print(message : Long) { System.out?.print(message) }
    fun print(message : Byte) { System.out?.print(message) }
    fun print(message : Short) { System.out?.print(message) }
    fun print(message : Char) { System.out?.print(message) }
    fun print(message : Boolean) { System.out?.print(message) }
    fun print(message : Float) { System.out?.print(message) }
    fun print(message : Double) { System.out?.print(message) }
    fun print(message : CharArray) { System.out?.print(message) }

    fun println(message : Any?) { System.out?.println(message) }
    fun println(message : Int) { System.out?.println(message) }
    fun println(message : Long) { System.out?.println(message) }
    fun println(message : Byte) { System.out?.println(message) }
    fun println(message : Short) { System.out?.println(message) }
    fun println(message : Char) { System.out?.println(message) }
    fun println(message : Boolean) { System.out?.println(message) }
    fun println(message : Float) { System.out?.println(message) }
    fun println(message : Double) { System.out?.println(message) }
    fun println(message : CharArray) { System.out?.println(message) }

    private var systemIn : InputStream? = null // Unfortunately, System.in may change
    private var stdin : BufferedReader? = null // This may introduce leaks of system.in objects...

    fun readLine() : String? {
    if (stdin == null || systemIn != System.`in`) {
        stdin = java.io.BufferedReader(java.io.InputStreamReader(System.`in`))
        systemIn = System.`in`
    }
    return stdin?.readLine()
    }
}