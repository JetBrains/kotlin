interface I1
interface I2

class C1 : I1
class C2 : I2

fun checkI1(o: Any): Boolean = o is I1
fun checkI2(o: Any): Boolean = o is I2
fun checkC1(o: Any): Boolean = o is C1
fun checkC2(o: Any): Boolean = o is C2

fun box(): String {
    val c1 = C1()
    val c2 = C2()
    val any = Any()

    if (!checkI1(c1)) return "FAIL1"
    if (checkI2(c1)) return "FAIL2"
    if (!checkC1(c1)) return "FAIL3"
    if (checkC2(c1)) return "FAIL4"

    if (checkI1(c2)) return "FAIL5"
    if (!checkI2(c2)) return "FAIL6"
    if (checkC1(c2)) return "FAIL7"
    if (!checkC2(c2)) return "FAIL8"

    if (checkI1(any)) return "FAIL9"
    if (checkI2(any)) return "FAIL10"
    if (checkC1(any)) return "FAIL11"
    if (checkC2(any)) return "FAIL12"

    return "OK"
}