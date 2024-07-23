// SEPARATE_SIGNATURE_DUMP_FOR_K2
// SKIP_KT_DUMP

// FILE: 1.kt

open class KotlinBase {
    var a: Int = 1
    var b: Int = 2
        protected set
    var c: Int = 3
        private set
    var d: Int = 4
        internal set
    open var e: Int = 5
}

class DirectChild: KotlinBase()
