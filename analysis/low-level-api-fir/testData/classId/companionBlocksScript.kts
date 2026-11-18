package one

/* ClassId: one/A */class A {
    companion {
        /* ClassId: one/A.B */class B
        /* ClassId: one/A.C */object C
        /* ClassId: one/A.D */interface D
        /* ClassId: one/A.E */enum class E
        /* ClassId: one/A.F */annotation class F
        /* ClassId: one/A.G */typealias G = Int

        /* ClassId: one/A.H */class H {
            companion {
                /* ClassId: one/A.H.I */class I
            }
        }
    }
}

/* ClassId: one/J */data class J(val i: Int) {
    companion {
        /* ClassId: one/J.K */class K
    }
}

/* ClassId: one/L */object L {
    companion {
        /* ClassId: one/L.M */class M
    }
}

/* ClassId: one/N */interface N {
    companion {
        /* ClassId: one/N.O */class O
    }
}

/* ClassId: one/P */enum class P {
    ;

    companion {
        /* ClassId: one/P.Q */class Q
    }
}

/* ClassId: one/R */class R {
    /* ClassId: one/R.Companion */companion object {
        /* ClassId: one/R.Companion.S */class S
    }

    companion {
        /* ClassId: one/R.T */class T
    }
}

fun foo() {
    /* ClassId: null */class U {
        companion {
            /* ClassId: null */class V
        }
    }
}
