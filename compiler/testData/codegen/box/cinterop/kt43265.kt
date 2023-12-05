// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: kt43265.def
strictEnums = bcm2835FunctionSelect
---
enum bcm2835FunctionSelect {
    BCM2835_GPIO_FSEL_INPT = 0x00, BCM2835_GPIO_FSEL_OUTP = 0x01, BCM2835_GPIO_FSEL_ALT0 = 0x04, BCM2835_GPIO_FSEL_ALT1 = 0x05,
    BCM2835_GPIO_FSEL_ALT2 = 0x06, BCM2835_GPIO_FSEL_ALT3 = 0x07, BCM2835_GPIO_FSEL_ALT4 = 0x03, BCM2835_GPIO_FSEL_ALT5 = 0x02,
    BCM2835_GPIO_FSEL_MASK = 0x07
};

// MODULE: main(cinterop)
// FILE: main.kt

import kt43265.*
import kotlin.test.*

@kotlinx.cinterop.ExperimentalForeignApi
fun box(): String {
    assertEquals(bcm2835FunctionSelect.BCM2835_GPIO_FSEL_ALT3, bcm2835FunctionSelect.BCM2835_GPIO_FSEL_MASK)

    return "OK"
}