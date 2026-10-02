package one.two

/* ClassId: one/two/<no name provided> */class {
    /* ClassId: one/two/<no name provided>.Companion */companion object {
        /* ClassId: one/two/<no name provided>.Companion.A */class A
    }
}

/* ClassId: one/two/<no name provided> */object {
    /* ClassId: one/two/<no name provided>.Named */class Named {
        /* ClassId: one/two/<no name provided>.Named.NamedCompanion */companion object NamedCompanion {
            /* ClassId: one/two/<no name provided>.Named.NamedCompanion.B */class B
            /* ClassId: one/two/<no name provided>.Named.NamedCompanion.<no name provided> */object {
                /* ClassId: one/two/<no name provided>.Named.NamedCompanion.<no name provided>.C */class C
            }
        }
    }
}

/* ClassId: one/two/Outer */class Outer {
    /* ClassId: one/two/Outer.<no name provided> */object {
        /* ClassId: one/two/Outer.<no name provided>.Inner */class Inner {
            companion {
                /* ClassId: one/two/Outer.<no name provided>.Inner.D */class D
                /* ClassId: one/two/Outer.<no name provided>.Inner.E */typealias E = Int
            }
        }
    }

    companion {
        /* ClassId: one/two/Outer.<no name provided> */object {
            /* ClassId: one/two/Outer.<no name provided>.F */class F
        }
    }
}
