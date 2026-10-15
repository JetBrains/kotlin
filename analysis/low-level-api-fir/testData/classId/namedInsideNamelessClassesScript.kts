package one.two

/* ClassId: null */object {
    /* ClassId: null */class A
    /* ClassId: null */object B
    /* ClassId: null */typealias C = Int
}

/* ClassId: one/two/Outer [PsiFqName: one.two.NamedInsideNamelessClassesScript.Outer] */class Outer {
    /* ClassId: one/two/Outer.<no name provided> [PsiFqName: one.two.NamedInsideNamelessClassesScript.Outer.<no name provided>] */object {
        /* ClassId: one/two/Outer.<no name provided>.A [PsiFqName: one.two.NamedInsideNamelessClassesScript.Outer.<no name provided>.A] */class A
        /* ClassId: one/two/Outer.<no name provided>.B [PsiFqName: one.two.NamedInsideNamelessClassesScript.Outer.<no name provided>.B] */object B
        /* ClassId: one/two/Outer.<no name provided>.C [PsiFqName: one.two.NamedInsideNamelessClassesScript.Outer.<no name provided>.C] */typealias C = Int
    }
}

// IGNORE_CONSISTENCY_CHECK: KT-61887
