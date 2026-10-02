package one.two

/* ClassId: one/two/<no name provided> [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.<no name provided>] */class {
    /* ClassId: one/two/<no name provided>.Companion [PsiFqName: null] */companion object {
        /* ClassId: one/two/<no name provided>.Companion.A [PsiFqName: null] */class A
    }
}

/* ClassId: null */object {
    /* ClassId: null */class Named {
        /* ClassId: null */companion object NamedCompanion {
            /* ClassId: null */class B
            /* ClassId: null */object {
                /* ClassId: null */class C
            }
        }
    }
}

/* ClassId: one/two/Outer [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.Outer] */class Outer {
    /* ClassId: one/two/Outer.<no name provided> [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.Outer.<no name provided>] */object {
        /* ClassId: one/two/Outer.<no name provided>.Inner [PsiFqName: null] */class Inner {
            companion {
                /* ClassId: one/two/Outer.<no name provided>.Inner.D [PsiFqName: null] */class D
                /* ClassId: one/two/Outer.<no name provided>.Inner.E [PsiFqName: null] */typealias E = Int
            }
        }
    }

    companion {
        /* ClassId: one/two/Outer.<no name provided> [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.Outer.<no name provided>] */object {
            /* ClassId: one/two/Outer.<no name provided>.F [PsiFqName: null] */class F
        }
    }
}

// IGNORE_CONSISTENCY_CHECK: KTIJ-26896, KT-61887
