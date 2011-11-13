namespace std

inline fun <T> array(vararg array : T) = array
inline fun array(vararg array : Byte) = array
inline fun array(vararg array : Char) = array
inline fun array(vararg array : Short) = array
inline fun array(vararg array : Int) = array
inline fun array(vararg array : Long) = array
inline fun array(vararg array : Double) = array
inline fun array(vararg array : Float) = array

fun Any?.identityEquals(other : Any?) = this === other

inline fun <T : Any> T?.sure() : T {
    if (this == null)
      throw NullPointerException()
    return this;
}

namespace string {
    fun String.replaceAllSubstrings(pattern : String, replacement : String) : String {
        return (this as java.lang.String).replace(pattern as CharSequence, replacement as CharSequence).sure()
    }

    fun String.replaceAllWithRegex(pattern : String, replacement : String) : String {
        return (this as java.lang.String).replaceAll(pattern, replacement).sure()
    }
}

namespace jutils {

    fun <T : Any> T.getJavaClass() : Class<T> {
        return ((this as Object).getClass()) as Class<T>
    }

}