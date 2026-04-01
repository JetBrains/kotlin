/* ClassId: /TopLevelObject [PsiFqName: ClassWithMembers.TopLevelObject] */object TopLevelObject

/* ClassId: /A [PsiFqName: ClassWithMembers.A] */class A constructor(a: Int) {
    /* ClassId: /A.B [PsiFqName: ClassWithMembers.A.B] */class B {
        /* ClassId: /A.B.C [PsiFqName: ClassWithMembers.A.B.C] */inner class C {
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

    /* ClassId: /A.NestedTypeAlias [PsiFqName: ClassWithMembers.A.NestedTypeAlias] */typealias NestedTypeAlias = B
}

/* ClassId: /TopLevelAlias [PsiFqName: ClassWithMembers.TopLevelAlias] */typealias TopLevelAlias

foo {
    /* ClassId: null */class E
}

// IGNORE_CONSISTENCY_CHECK: KTIJ-26902