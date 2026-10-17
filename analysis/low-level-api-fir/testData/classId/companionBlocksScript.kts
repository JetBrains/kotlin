// IGNORE_CONSISTENCY_CHECK: KT-61887
package one

/* ClassId: one/A [PsiFqName: one.CompanionBlocksScript.A] */class A {
    companion {
        /* ClassId: one/A.B [PsiFqName: one.CompanionBlocksScript.A.B] */class B
        /* ClassId: one/A.C [PsiFqName: one.CompanionBlocksScript.A.C] */object C
        /* ClassId: one/A.D [PsiFqName: one.CompanionBlocksScript.A.D] */interface D
        /* ClassId: one/A.E [PsiFqName: one.CompanionBlocksScript.A.E] */enum class E
        /* ClassId: one/A.F [PsiFqName: one.CompanionBlocksScript.A.F] */annotation class F
        /* ClassId: one/A.G [PsiFqName: one.CompanionBlocksScript.A.G] */typealias G = Int

        /* ClassId: one/A.H [PsiFqName: one.CompanionBlocksScript.A.H] */class H {
            companion {
                /* ClassId: one/A.H.I [PsiFqName: one.CompanionBlocksScript.A.H.I] */class I
            }
        }
    }
}

/* ClassId: one/J [PsiFqName: one.CompanionBlocksScript.J] */data class J(val i: Int) {
    companion {
        /* ClassId: one/J.K [PsiFqName: one.CompanionBlocksScript.J.K] */class K
    }
}

/* ClassId: one/L [PsiFqName: one.CompanionBlocksScript.L] */object L {
    companion {
        /* ClassId: one/L.M [PsiFqName: one.CompanionBlocksScript.L.M] */class M
    }
}

/* ClassId: one/N [PsiFqName: one.CompanionBlocksScript.N] */interface N {
    companion {
        /* ClassId: one/N.O [PsiFqName: one.CompanionBlocksScript.N.O] */class O
    }
}

/* ClassId: one/P [PsiFqName: one.CompanionBlocksScript.P] */enum class P {
    ;

    companion {
        /* ClassId: one/P.Q [PsiFqName: one.CompanionBlocksScript.P.Q] */class Q
    }
}

/* ClassId: one/R [PsiFqName: one.CompanionBlocksScript.R] */class R {
    /* ClassId: one/R.Companion [PsiFqName: one.CompanionBlocksScript.R.Companion] */companion object {
        /* ClassId: one/R.Companion.S [PsiFqName: one.CompanionBlocksScript.R.Companion.S] */class S
    }

    companion {
        /* ClassId: one/R.T [PsiFqName: one.CompanionBlocksScript.R.T] */class T
    }
}

fun foo() {
    /* ClassId: null */class U {
        companion {
            /* ClassId: null */class V
        }
    }
}
