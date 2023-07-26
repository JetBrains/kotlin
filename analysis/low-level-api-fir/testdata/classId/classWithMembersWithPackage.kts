package one.two

/* ClassId: one/two/TopLevelObject */object TopLevelObject

/* ClassId: one/two/A */class A constructor(a: Int) {
    /* ClassId: one/two/A.B */class B {
        /* ClassId: one/two/A.B.C */inner class C {
            fun foo() {
                /* ClassId: null */typealias F = C

                /* ClassId: null */class BBA
            }
        }
    }

    fun boo() {
        /* ClassId: null */class A
        /* ClassId: null */typealias B = A
    }

    /* ClassId: one/two/A.NestedTypeAlias */typealias NestedTypeAlias = B
}

/* ClassId: one/two/TopLevelAlias */typealias TopLevelAlias

foo {
    /* ClassId: null */class E
}
