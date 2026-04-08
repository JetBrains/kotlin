// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_FIR_METADATA_LOADING_K1

package test

class C {
    companion {
        fun foo(s: String): Int = 1
    }

    companion {
        val x get() = 1
        var y = false
        var z: String
            get() = ""
            set(value) {}
    }
}

companion fun C.bar(x: Boolean) {}
companion val C.xx get() = 2
companion var C.yy = false
companion var C.zz: String
    get() = ""
    set(value) {}