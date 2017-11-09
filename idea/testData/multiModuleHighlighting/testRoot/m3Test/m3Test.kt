package shared.test

import shared.*

private fun privateInM3Test() {
}
internal fun internalInM3Test() {
}
public fun publicInM3Test() {
}

fun access() {
    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM1': it is private in file">privateInM1</error>()
    <error descr="[INVISIBLE_MEMBER] Cannot access 'internalInM1': it is internal in 'shared'">internalInM1</error>()
    publicInM1()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM1Test': it is private in file">privateInM1Test</error>()
    <error descr="[INVISIBLE_MEMBER] Cannot access 'internalInM1Test': it is internal in 'shared.test'">internalInM1Test</error>()
    publicInM1Test()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM2': it is private in file">privateInM2</error>()
    <error descr="[INVISIBLE_MEMBER] Cannot access 'internalInM2': it is internal in 'shared'">internalInM2</error>()
    publicInM2()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM2Test': it is private in file">privateInM2Test</error>()
    <error descr="[INVISIBLE_MEMBER] Cannot access 'internalInM2Test': it is internal in 'shared.test'">internalInM2Test</error>()
    publicInM2Test()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM3': it is private in file">privateInM3</error>()
    internalInM3()
    publicInM3()

    privateInM3Test()
    internalInM3Test()
    publicInM3Test()
}
