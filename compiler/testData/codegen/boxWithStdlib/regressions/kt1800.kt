//KT-1800 error/NonExistentClass generated on runtime
package i

import java.util.ArrayList

public class User(val firstName: String,
                  val lastName: String,
                  val age: Int) {
    fun toString() = "$firstName $lastName, age $age"
}

public fun <T: Comparable<T>> Collection<T>.min(): T? {
    var minValue: T? = null
    for(value in this) {
        if (minValue == null || value.compareTo(minValue!!) < 0) {
            minValue = value
        }
    }
    return minValue
}

fun box() : String {
    val users = arrayList(
            User("John", "Doe", 30),
            User("Jane", "Doe", 27))

    val ages = users.map { it.age }

    val minAge = ages.min()
    return if (minAge == 27) "OK" else "fail"
}
