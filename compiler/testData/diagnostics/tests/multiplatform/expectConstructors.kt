// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FIR_DUMP

// MODULE: m1-common
// FILE: common.kt

expect sealed class Owner {
    class WithoutConstructor : Owner {
        constructor(arg: Int)
    }

    class WithConstructor public constructor(arg: Int) : Owner {
        constructor(s: String)
    }
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual sealed class Owner {
    actual class WithoutConstructor : Owner {
        actual constructor(arg: Int) : super()
    }

    actual class WithConstructor actual constructor(val arg: Int) : Owner() {
        actual constructor(s: String) : this(0)
    }
}
