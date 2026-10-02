package one.two

/* ClassId: one/two/<no name provided> */object {
    /* ClassId: one/two/<no name provided>.A [PsiFqName: null] */class A
    /* ClassId: one/two/<no name provided>.B [PsiFqName: null] */object B
    /* ClassId: one/two/<no name provided>.C [PsiFqName: null] */typealias C = Int
}

/* ClassId: one/two/Outer */class Outer {
    /* ClassId: one/two/Outer.<no name provided> */object {
        /* ClassId: one/two/Outer.<no name provided>.A [PsiFqName: null] */class A
        /* ClassId: one/two/Outer.<no name provided>.B [PsiFqName: null] */object B
        /* ClassId: one/two/Outer.<no name provided>.C [PsiFqName: null] */typealias C = Int
    }
}

// IGNORE_CONSISTENCY_CHECK: KTIJ-26896
