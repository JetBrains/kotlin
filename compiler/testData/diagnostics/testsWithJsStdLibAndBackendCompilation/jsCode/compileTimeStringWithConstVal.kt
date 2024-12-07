// FIR_IDENTICAL
// MODULE: lib1
// FILE: A.kt
const val constFromOtherModule = "constFromOtherModule"

class ClassFromOtherModule {
    companion object {
        const val constFromOtherModuleCompanion = "constFromOtherModuleCompanion"
    }
}

// MODULE: main(lib1)
// FILE: A.kt
const val constFromOtherFile = "constFromOtherFile"

class ClassFromOtherFile {
    companion object {
        const val constFromOtherFileCompanion = "constFromOtherFileCompanion"
    }
}

// FILE: Main.kt
const val constFromThisFile = "constFromThisFile"

open class ClassFromThisFile {
    companion object {
        const val constFromThisFileCompanion = "constFromThisFileCompanion"

        fun testCompanionFromThisCompanion() {
            js("var ${constFromThisFileCompanion} = 1;")
            js("var " + constFromThisFileCompanion + " = 1;")
        }
    }

    fun testCompanionFromThis() {
        js("var ${constFromThisFileCompanion} = 1;")
        js("var " + constFromThisFileCompanion + " = 1;")
    }
}

class Class: ClassFromThisFile() {
    fun testCompanionFromParent() {
        js("var ${constFromThisFileCompanion} = 1;")
        js("var " + constFromThisFileCompanion + " = 1;")
    }
}

fun testConstFromOtherModule() {
    js("var $constFromOtherModule = 1;")
    js("var " + constFromOtherModule + " = 1;")
}

fun testCompanionConstFromOtherModule() {
    js("var ${ClassFromOtherModule.constFromOtherModuleCompanion} = 1;")
    js("var " + ClassFromOtherModule.constFromOtherModuleCompanion + " = 1;")
}

fun testConstFromOtherFile() {
    js("var $constFromOtherFile = 1;")
    js("var " + constFromOtherFile + " = 1;")
}

fun testCompanionConstFromOtherFile() {
    js("var ${ClassFromOtherFile.constFromOtherFileCompanion} = 1;")
    js("var " + ClassFromOtherFile.constFromOtherFileCompanion + " = 1;")
}

fun testConstFromThisFile() {
    js("var $constFromThisFile = 1;")
    js("var " + constFromThisFile + " = 1;")
}

fun testCompanionConstFromThisFile() {
    js("var ${ClassFromThisFile.constFromThisFileCompanion} = 1;")
    js("var " + ClassFromThisFile.constFromThisFileCompanion + " = 1;")
}
