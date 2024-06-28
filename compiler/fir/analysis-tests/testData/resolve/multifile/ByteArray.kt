// FILE: byteArray.kt

package Test

interface ByteArray {
    val array: ByteArray
}

// FILE: main.kt

package use

import <!UNRESOLVED_IMPORT!>test<!>.*

interface My {
    // Should be kotlin.ByteArray
    val array: ByteArray
}
