package shared

import shared.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: test">test</error>.*

private fun privateInM2() {
}
internal fun internalInM2() {
}
public fun publicInM2() {
}

fun access() {
    <error descr="[INVISIBLE_MEMBER] Cannot access 'privateInM1': it is private in file">privateInM1</error>()
    <error descr="[INVISIBLE_MEMBER] Cannot access 'internalInM1': it is internal in 'shared'">internalInM1</error>()
    publicInM1()

    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: privateInM1Test">privateInM1Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: internalInM1Test">internalInM1Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: publicInM1Test">publicInM1Test</error>()
    
    privateInM2()
    internalInM2()
    publicInM2()
    
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: privateInM2Test">privateInM2Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: internalInM2Test">internalInM2Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: publicInM2Test">publicInM2Test</error>()
    
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: privateInM3">privateInM3</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: internalInM3">internalInM3</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: publicInM3">publicInM3</error>()
    
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: privateInM3Test">privateInM3Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: internalInM3Test">internalInM3Test</error>()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: publicInM3Test">publicInM3Test</error>()
}
