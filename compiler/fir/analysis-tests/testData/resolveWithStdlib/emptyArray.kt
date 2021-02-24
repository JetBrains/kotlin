val x: Array<String> = emptyArray()

val y: Array<String>
    get() = emptyArray()

interface My

val z: Array<out My>
    get() = emptyArray()