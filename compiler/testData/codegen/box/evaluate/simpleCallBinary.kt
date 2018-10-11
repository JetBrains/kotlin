// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p3: Int,
        val p4: Int,
        val p5: Int
)

const val prop1: Int = 1.plus(1)
const val prop2: Int = 1.minus(1)
const val prop3: Int = 1.times(1)
const val prop4: Int = 1.div(1)
const val prop5: Int = 1.rem(1)

@Ann(prop1, prop2, prop3, prop4, prop5) class MyClass

fun box(): String {
    val annotation = MyClass::class.java.getAnnotation(Ann::class.java)!!
    if (annotation.p1 != prop1) return "fail 1, expected = ${prop1}, actual = ${annotation.p1}"
    if (annotation.p2 != prop2) return "fail 2, expected = ${prop2}, actual = ${annotation.p2}"
    if (annotation.p3 != prop3) return "fail 3, expected = ${prop3}, actual = ${annotation.p3}"
    if (annotation.p4 != prop4) return "fail 4, expected = ${prop4}, actual = ${annotation.p4}"
    if (annotation.p5 != prop5) return "fail 5, expected = ${prop5}, actual = ${annotation.p5}"
    return "OK"
}
