package one.two

/* ClassId: null */object {
    /* ClassId: null */class A
    /* ClassId: null */object B
    /* ClassId: null */typealias C = Int
}

/* ClassId: one/two/Outer [PsiFqName: one.two.NamedInsideNamelessClassesScript.Outer] */class Outer {
    /* ClassId: one/two/Outer.<no name provided> [PsiFqName: one.two.NamedInsideNamelessClassesScript.Outer.<no name provided>] */object {
        /* ClassId: one/two/Outer.<no name provided>.A [PsiFqName: null] */class A
        /* ClassId: one/two/Outer.<no name provided>.B [PsiFqName: null] */object B
        /* ClassId: one/two/Outer.<no name provided>.C [PsiFqName: null] */typealias C = Int
    }
}

// IGNORE_CONSISTENCY_CHECK: KTIJ-26896, KT-61887
