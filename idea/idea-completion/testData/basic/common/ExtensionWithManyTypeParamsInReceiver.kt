open class Base

class ManySome<T, U>
fun <P1, P2>ManySome<P1, P2>.testExactGeneralGeneral() = "Some"
fun <P1, P2>ManySome<P1, Int>.testExactGeneralInt() = "Some"
fun <P1, P2>ManySome<Int, Int>.testExactIntInt() = "Some"
fun <P1 : Base, P2>ManySome<P1, Int>.testSubBaseIntInt() = "Some"
fun <P1, P2, P3>ManySome<P1, P2>.testManyGeneralGeneral() = "Some"

fun some() {
    ManySome<Int, Int>().test<caret>
}

// EXIST: testExactGeneralGeneral
// EXIST: testExactGeneralInt
// EXIST: testExactIntInt
// EXIST: testManyGeneralGeneral
// ABSENT: testSubBaseIntInt
