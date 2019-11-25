// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
//KT-1800 error/NonExistentClass generated on runtime
package i

public class User(val firstName: String,
                  val lastName: String,
                  val age: Int) {
    override fun toString() = "$firstName $lastName, age $age"
}

public fun <T: Comparable<T>> Collection<T>.testMin(): T? {
    var minValue: T? = null
    for(value in this) {
        if (minValue == null || value.compareTo(minValue!!) < 0) {
            minValue = value
        }
    }
    return minValue
}

fun box() : String {
    val users = arrayListOf(
            User("John", "Doe", 30),
            User("Jane", "Doe", 27))

    val ages = users.map { it.age }

    val minAge = ages.testMin()
    return if (minAge == 27) "OK" else "fail"
}
