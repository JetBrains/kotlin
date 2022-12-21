// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

@JvmInline
value class ICInt(val x: Int)

@JvmInline
value class ICIntNullable(val x: Int?)

@JvmInline
value class ICString(val x: String)

@JvmInline
value class ICStringNullable(val x: String?)

@JvmInline
value class ICICInt(val x: ICInt)

@JvmInline
value class ICICIntNullable(val x: ICInt?)

fun foo1(vararg p: ICInt) = p[1]
fun bar1(vararg p: ICInt?) = p[1]

fun foo2(vararg p: ICIntNullable) = p[1]
fun bar2(vararg p: ICIntNullable?) = p[1]

fun foo3(vararg p: ICString) = p[1]
fun bar3(vararg p: ICString?) = p[1]

fun foo4(vararg p: ICStringNullable) = p[1]
fun bar4(vararg p: ICStringNullable?) = p[1]

fun foo5(vararg p: ICICInt) = p[1]
fun bar5(vararg p: ICICInt?) = p[1]

fun foo6(vararg p: ICICIntNullable) = p[1]
fun bar6(vararg p: ICICIntNullable?) = p[1]

fun box(): String {
    if (foo1(ICInt(2), ICInt(3), ICInt(5)).x != 3) return "Fail 1.1"
    if (bar1(null, ICInt(3), ICInt(5))?.x != 3) return "Fail 1.2"

    if (foo2(ICIntNullable(2), ICIntNullable(null), ICIntNullable(5)).x != null) return "Fail 2.1"
    if (bar2(null, ICIntNullable(3), ICIntNullable(null))?.x != 3) return "Fail 2.2"

    if (foo3(ICString("a"), ICString("b"), ICString("c")).x != "b") return "Fail 3.1"
    if (bar3(null, ICString("b"), ICString("c"))?.x != "b") return "Fail 3.2"

    if (foo4(ICStringNullable("a"), ICStringNullable(null), ICStringNullable("c")).x != null) return "Fail 4.1"
    if (bar4(null, ICStringNullable("b"), ICStringNullable(null))?.x != "b") return "Fail 4.2"

    if (foo5(ICICInt(ICInt(2)), ICICInt(ICInt(3)), ICICInt(ICInt(5))).x.x != 3) return "Fail 5.1"
    if (bar5(null, ICICInt(ICInt(3)), ICICInt(ICInt(5)))?.x?.x != 3) return "Fail 5.2"

    if (foo6(ICICIntNullable(ICInt(2)), ICICIntNullable(null), ICICIntNullable(ICInt(5))).x != null) return "Fail 6.1"
    if (bar6(null, ICICIntNullable(ICInt(3)), ICICIntNullable(null))?.x?.x != 3) return "Fail 6.2"

    return "OK"
}