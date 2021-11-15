// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
import java.util.*

fun box(): String {
    fun f(): Any {
        fun g() = object {}
        return g()
    }

    fun F(): Any {
        fun g() = object {}
        return g()
    }

    val class1 = f().javaClass.name
    val class2 = F().javaClass.name

    if (class1.uppercase(Locale.ENGLISH) != class2.uppercase(Locale.ENGLISH))
        return "OK"
    else
        return "$class1 $class2"
}
