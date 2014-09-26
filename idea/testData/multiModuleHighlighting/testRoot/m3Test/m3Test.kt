package shared.test

import shared.*

private fun privateInM3Test() {
}
fun internalInM3Test() {
}
public fun publicInM3Test() {
}

fun access() {
    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM1': it is 'private' in 'shared'">privateInM1</error>()
    internalInM1()
    publicInM1()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM1Test': it is 'private' in 'test'">privateInM1Test</error>()
    internalInM1Test()
    publicInM1Test()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM2': it is 'private' in 'shared'">privateInM2</error>()
    internalInM2()
    publicInM2()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM2Test': it is 'private' in 'test'">privateInM2Test</error>()
    internalInM2Test()
    publicInM2Test()

    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM3': it is 'private' in 'shared'">privateInM3</error>()
    internalInM3()
    publicInM3()

    privateInM3Test()
    internalInM3Test()
    publicInM3Test()
}