// TARGET_BACKEND: JVM_IR
// ISSUE: KT-62598

// MODULE: m1
// FILE: m1.kt
interface Holder {
    interface Entry {
        @Annotation(value = [""])
        fun f()
    }

    annotation class Annotation(
        val value: Array<String>,
    )
}

interface ByteHolder {
    interface Entry {
        @Annotation(value = [1])
        fun f()
    }

    annotation class Annotation(
        val value: ByteArray,
    )
}

interface HolderWithDefault {
    interface Entry {
        @Annotation
        fun f()
    }

    annotation class Annotation(
        val value: Array<String> = [""],
    )
}

interface HolderWithEmpty {
    interface Entry {
        @Annotation(value = [])
        fun f()
    }

    annotation class Annotation(
        val value: Array<String>,
    )
}

// MODULE: m2(m1)
// FILE: m2.kt
import Holder
import ByteHolder
import HolderWithDefault
import HolderWithEmpty

fun box() = "OK"
