package one.two

/* ClassId: one/two/<no name provided> */class {
    /* ClassId: one/two/<no name provided>.Companion [PsiFqName: null] */companion object {
        /* ClassId: one/two/<no name provided>.Companion.A [PsiFqName: null] */class A
    }
}

/* ClassId: one/two/<no name provided> */object {
    /* ClassId: one/two/<no name provided>.Named [PsiFqName: null] */class Named {
        /* ClassId: one/two/<no name provided>.Named.NamedCompanion [PsiFqName: null] */companion object NamedCompanion {
            /* ClassId: one/two/<no name provided>.Named.NamedCompanion.B [PsiFqName: null] */class B
            /* ClassId: one/two/<no name provided>.Named.NamedCompanion.<no name provided> [PsiFqName: null] */object {
                /* ClassId: one/two/<no name provided>.Named.NamedCompanion.<no name provided>.C [PsiFqName: null] */class C
            }
        }
    }
}

/* ClassId: one/two/Outer */class Outer {
    /* ClassId: one/two/Outer.<no name provided> */object {
        /* ClassId: one/two/Outer.<no name provided>.Inner [PsiFqName: null] */class Inner {
            companion {
                /* ClassId: one/two/Outer.<no name provided>.Inner.D [PsiFqName: null] */class D
                /* ClassId: one/two/Outer.<no name provided>.Inner.E [PsiFqName: null] */typealias E = Int
            }
        }
    }

    companion {
        /* ClassId: one/two/Outer.<no name provided> */object {
            /* ClassId: one/two/Outer.<no name provided>.F [PsiFqName: null] */class F
        }
    }
}

// IGNORE_CONSISTENCY_CHECK: KTIJ-26896
