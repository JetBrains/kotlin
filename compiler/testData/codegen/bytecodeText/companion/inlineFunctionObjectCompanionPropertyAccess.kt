class A {
    companion object {
        val s = "OK"
        var v = "NOT OK"
    }

    inline fun g(crossinline f: () -> Unit) {
        {
            f()
            s
            v = "OK"
        }.let { it() }
    }

    inline fun g2(crossinline f: () -> Unit) {
        object {
            fun run() {
                f()
                s
                v = "OK"
            }
        }.run()
    }

    inline fun use() {
        g {
            s
            g2 { s }
        }
        g {
            v = "OK"
            g2 {
                v = "OK"
            }
        }
    }

    fun useNonInline() {
        g {
            s
            g2 { s }
        }
        g {
            v = "OK"
            g2 {
                v = "OK"
            }
        }
    }
}

// One direct `A.s` access in the accessibility bridge `access$getS$cp`.
// 1 GETSTATIC A.s

// One direct `A.v` set in the accessibility bridge `access$setV$cp`.
// One direct `A.v` set in `A.<clinit>`
// 2 PUTSTATIC A.v

// JVM_IR_TEMPLATES

// Two accesses from the inline function code for `g` and `g2`.
// Four accesses from the code for `g` and `g2` inlined in `useInline`.
// Two accesses from the lambdas inlined in `useInline`.
// Four accesses from the code for `g` and `g2` inlined in `useNonInline`.
// 12 INVOKEVIRTUAL A\$Companion.getS

// Two accesses from the inline function code for `g` and `g2`.
// Four accesses from the code for `g` and `g2` inlined in `useInline`.
// Two accesses from the lambdas inlined in `useInline`.
// Four accesses from the code for `g` and `g2` inlined in `useNonInline`.
// 12 INVOKEVIRTUAL A\$Companion.setV

// One call to the accessibility bridge `access$setV$cp` from Companion.setV.
// Two uses of the direct accessor from the lambdas inlined in useNonInline.
// 3 INVOKESTATIC A.access\$setV\$cp

// One call to the accessibility bridge `access$getS$cp` from Companion.getS.
// Two uses of the direct accessor from the lambdas inlined in useNonInline.
// 3 INVOKESTATIC A.access\$getS\$cp

// JVM_TEMPLATES

// Two accesses from the inline function code for `g` and `g2`.
// Four accesses from the code for `g` and `g2` inlined in `useInline`.
// Two accesses from the lambdas inlined in `useInline`.
// Four accesses from the code for `g` and `g2` inlined in `useNonInline`.
// Two accesses from the lambdas inlined in `useNonInline`.
// 14 INVOKEVIRTUAL A\$Companion.getS

// Two accesses from the inline function code for `g` and `g2`.
// Four accesses from the code for `g` and `g2` inlined in `useInline`.
// Two accesses from the lambdas inlined in `useInline`.
// Four accesses from the code for `g` and `g2` inlined in `useNonInline`.
// Two accesses from the lambdas inlined in `useNonInline`.
// 14 INVOKEVIRTUAL A\$Companion.setV

// One call to the accessibility bridge `access$setV$cp` from Companion.setV.
// 1 INVOKESTATIC A.access\$setV\$cp

// One call to the accessibility bridge `access$getS$cp` from Companion.getS.
// 1 INVOKESTATIC A.access\$getS\$cp