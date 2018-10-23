@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Foo

fun box(): String? {
    val lambda_1 = <!ELEMENT(1)!>@ {
        return@<!ELEMENT(1)!> false
    }
    val lambda_2 = @Foo <!ELEMENT(2)!>@ { Boolean
        return@<!ELEMENT(2)!> false
    }

    val x1 = <!ELEMENT(1)!>@ true

    var i = 0

    <!ELEMENT(2)!>@ while (true) {
        i++
        if (i < 4) continue@<!ELEMENT(2)!>
        i++
        if (i > 15) break@<!ELEMENT(2)!>
    }

    var j = 0

    <!ELEMENT(1)!>@ for (k in 0..20) {
        j++
        if (k < 4) continue@<!ELEMENT(1)!>
        j++
        if (k > 15) break@<!ELEMENT(1)!>
    }

    if (lambda_1()) return null
    if (lambda_2()) return null
    if (!x1) return null
    if (i != 17) return null
    if (j != 30) return null

    return "OK"
}
