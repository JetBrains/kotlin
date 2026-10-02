package one.two

/* ClassId: one/two/A */class A {
    /* ClassId: one/two/A.<no name provided> */object {
        /* ClassId: one/two/A.<no name provided>.B [PsiFqName: null] */class B {
            /* ClassId: one/two/A.<no name provided>.B.<no name provided> [PsiFqName: null] */object {
                /* ClassId: one/two/A.<no name provided>.B.<no name provided>.C [PsiFqName: null] */class C
                /* ClassId: one/two/A.<no name provided>.B.<no name provided>.<no name provided> [PsiFqName: null] */object {
                    /* ClassId: one/two/A.<no name provided>.B.<no name provided>.<no name provided>.D [PsiFqName: null] */class D
                }
            }
        }
    }
}

/* ClassId: one/two/<no name provided> */object {
    /* ClassId: one/two/<no name provided>.<no name provided> [PsiFqName: null] */class {
        /* ClassId: one/two/<no name provided>.<no name provided>.<no name provided> [PsiFqName: null] */object {
            /* ClassId: one/two/<no name provided>.<no name provided>.<no name provided>.E [PsiFqName: null] */class E
        }
    }

    /* ClassId: one/two/<no name provided>.F [PsiFqName: null] */class F {
        /* ClassId: one/two/<no name provided>.F.<no name provided> [PsiFqName: null] */class {
            /* ClassId: one/two/<no name provided>.F.<no name provided>.G [PsiFqName: null] */class G
        }
    }
}

// IGNORE_CONSISTENCY_CHECK: KTIJ-26896
