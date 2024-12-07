// LANGUAGE: +LightweightLambdas
// TARGET_BACKEND: JVM_IR

fun <T> block(t: T, f: T.() -> Unit) {
    f.invoke(t)
}

fun test() {
    block("first") place1@ {
        block("second") place2@ {
            // Breakpoint here
            this@place1
            this@place2
        }
    }
}

// 2 INVOKEDYNAMIC
// 0 receiver
// 5 LOCALVARIABLE
// 1 LOCALVARIABLE \$this\$place1
// 1 LOCALVARIABLE \$this\$place2
// 1 LOCALVARIABLE t
// 1 LOCALVARIABLE f
// 1 LOCALVARIABLE \$this_place1
// 1 LDC "\$this\$place1"
// 1 LDC "\$this\$place2"
