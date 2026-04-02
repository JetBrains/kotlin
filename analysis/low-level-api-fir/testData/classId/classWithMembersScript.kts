/* ClassId: /TopLevelObject [PsiFqName: ClassWithMembersScript.TopLevelObject] */object TopLevelObject

/* ClassId: /A [PsiFqName: ClassWithMembersScript.A] */class A constructor(a: Int) {
    /* ClassId: /A.B [PsiFqName: ClassWithMembersScript.A.B] */class B {
        /* ClassId: /A.B.C [PsiFqName: ClassWithMembersScript.A.B.C] */inner class C {
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

    /* ClassId: /A.NestedTypeAlias [PsiFqName: ClassWithMembersScript.A.NestedTypeAlias] */typealias NestedTypeAlias = B
}

/* ClassId: /TopLevelAlias [PsiFqName: ClassWithMembersScript.TopLevelAlias] */typealias TopLevelAlias

foo {
    /* ClassId: null */class E
}

// IGNORE_CONSISTENCY_CHECK: KTIJ-26902