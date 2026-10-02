package one.two

/* ClassId: one/two/A [PsiFqName: one.two.DeepNestingInsideNamelessClassesScript.A] */class A {
    /* ClassId: one/two/A.<no name provided> [PsiFqName: one.two.DeepNestingInsideNamelessClassesScript.A.<no name provided>] */object {
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

/* ClassId: null */object {
    /* ClassId: null */class {
        /* ClassId: null */object {
            /* ClassId: null */class E
        }
    }

    /* ClassId: null */class F {
        /* ClassId: null */class {
            /* ClassId: null */class G
        }
    }
}

// IGNORE_CONSISTENCY_CHECK: KTIJ-26896, KT-61887
