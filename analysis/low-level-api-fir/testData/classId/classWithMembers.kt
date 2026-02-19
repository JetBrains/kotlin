package one

/* ClassId: one/TopLevelObject */object TopLevelObject

/* ClassId: one/A */class A constructor(a: Int) {
    /* ClassId: one/A.B */class B {
        /* ClassId: one/A.B.C */inner class C {
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

    /* ClassId: one/A.NestedTypeAlias */typealias NestedTypeAlias = B
}

/* ClassId: one/TopLevelAlias */typealias TopLevelAlias
