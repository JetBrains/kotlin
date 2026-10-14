package one.two

/* ClassId: one/two/<no name provided> [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.<no name provided>] */class {
    /* ClassId: one/two/<no name provided>.Companion [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.<no name provided>.Companion] */companion object {
        /* ClassId: one/two/<no name provided>.Companion.A [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.<no name provided>.Companion.A] */class A
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
        /* ClassId: one/two/Outer.<no name provided>.Inner [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.Outer.<no name provided>.Inner] */class Inner {
            companion {
                /* ClassId: one/two/Outer.<no name provided>.Inner.D [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.Outer.<no name provided>.Inner.D] */class D
                /* ClassId: one/two/Outer.<no name provided>.Inner.E [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.Outer.<no name provided>.Inner.E] */typealias E = Int
            }
        }
    }

    companion {
        /* ClassId: one/two/Outer.<no name provided> [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.Outer.<no name provided>] */object {
            /* ClassId: one/two/Outer.<no name provided>.F [PsiFqName: one.two.CompanionsInsideNamelessClassesScript.Outer.<no name provided>.F] */class F
        }
    }
}

// IGNORE_CONSISTENCY_CHECK: KT-61887
