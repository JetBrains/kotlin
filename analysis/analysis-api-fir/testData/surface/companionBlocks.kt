// ATTACH_LIBRARY_JAR: companionBlocksLib.jar

import org.example.*

fun test(foo: Foo) {
    val ret1 = foo.ordinaryProperty
    val ret2 = foo.ordinaryFunction()
    val ret3 = foo.companionBlockProperty
    val ret4 = foo.companionBlockFunction()
    val ret5 = Foo.companionBlockProperty
    val ret6 = Foo.companionBlockFunction()
    val ret7 = String.empty
    val ret8 = String.fromNumber(9001)
    val ret9 = Direction.values()
    val ret10 = Direction.entries
    val ret11 = Direction.valueOf("SOUTH")
}