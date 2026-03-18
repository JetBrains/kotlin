// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM

// MODULE: lib-common
expect class A<T>

// MODULE: lib-inter()()(lib-common)
typealias AInter<T> = A<T>

// MODULE: lib-platform()()(lib-inter)
import java.util.ArrayList

actual typealias A<T> = ArrayList<T>
typealias APlatform<T> = AInter<T>


// MODULE: app-common(lib-common)
fun acceptA(a: A<String>): A<String> = a

// MODULE: app-inter(lib-inter)(lib-common)(app-common)
fun acceptAInter(a: AInter<String>): AInter<String> = a


// MODULE: app-platform(lib-platform)()(app-inter)
fun acceptAPlatform(a: APlatform<String>): APlatform<String> = a

fun box(): String {
    val a = A<String>()
    val c1 = acceptA(a)
    val c2 = acceptAInter(c1)
    val c3 = acceptAPlatform(c2)
    return if (a === c3) "OK" else "Fail"
}
