// TARGET_BACKEND: JVM

// WITH_STDLIB

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p3: Int,
        val p4: Int,
        val p5: Long,
        val p6: Long
)

@Ann(
        p1 = <!EVALUATED("128")!>java.lang.Byte.MAX_VALUE + 1<!>,
        p2 = <!EVALUATED("32768")!>java.lang.Short.MAX_VALUE + 1<!>,
        p3 = <!EVALUATED("-2147483648")!>java.lang.Integer.MAX_VALUE + 1<!>,
        p4 = <!EVALUATED("-2147483648")!>java.lang.Integer.MAX_VALUE + 1<!>,
        p5 = <!EVALUATED("2147483648")!>java.lang.Integer.MAX_VALUE + 1.toLong()<!>,
        p6 = <!EVALUATED("-9223372036854775808")!>java.lang.Long.MAX_VALUE + 1<!>
) class MyClass

fun box(): String {
    val annotation = MyClass::class.java.getAnnotation(Ann::class.java)!!
    if (annotation.p1 != 128) return "fail 1, expected = ${128}, actual = ${annotation.p1}"
    if (annotation.p2 != 32768) return "fail 2, expected = ${32768}, actual = ${annotation.p2}"
    if (annotation.p3 != -2147483648) return "fail 3, expected = ${-2147483648}, actual = ${annotation.p3}"
    if (annotation.p4 != -2147483648) return "fail 4, expected = ${-2147483648}, actual = ${annotation.p4}"
    if (annotation.p5 != 2147483648.toLong()) return "fail 5, expected = ${2147483648}, actual = ${annotation.p5}"
    if (annotation.p6 != java.lang.Long.MAX_VALUE + 1) return "fail 5, expected = ${java.lang.Long.MAX_VALUE + 1}, actual = ${annotation.p6}"
    return "OK"
}
