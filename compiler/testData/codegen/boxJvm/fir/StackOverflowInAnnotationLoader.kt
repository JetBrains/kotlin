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

interface HolderWithTypeAnnotation {
    interface Entry {
        fun f(): @Annotation(value = [""]) Unit
    }

    @Target(AnnotationTarget.TYPE)
    annotation class Annotation(
        val value: Array<String>,
    )
}

// MODULE: m2(m1)
// FILE: m2.kt

fun box(): String {
    object : Holder {}
    object : ByteHolder {}
    object : HolderWithDefault {}
    object : HolderWithEmpty {}
    object : HolderWithTypeAnnotation {}
    return "OK"
}
