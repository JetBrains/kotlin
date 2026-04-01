// IGNORE_CONSISTENCY_CHECK: KTIJ-26902
package one.two

/* ClassId: one/two/TopLevelObject [PsiFqName: one.two.ClassWithMembersWithPackage.TopLevelObject] */object TopLevelObject

/* ClassId: one/two/A [PsiFqName: one.two.ClassWithMembersWithPackage.A] */class A constructor(a: Int) {
    /* ClassId: one/two/A.B [PsiFqName: one.two.ClassWithMembersWithPackage.A.B] */class B {
        /* ClassId: one/two/A.B.C [PsiFqName: one.two.ClassWithMembersWithPackage.A.B.C] */inner class C {
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

    /* ClassId: one/two/A.NestedTypeAlias [PsiFqName: one.two.ClassWithMembersWithPackage.A.NestedTypeAlias] */typealias NestedTypeAlias = B
}

/* ClassId: one/two/TopLevelAlias [PsiFqName: one.two.ClassWithMembersWithPackage.TopLevelAlias] */typealias TopLevelAlias

foo {
    /* ClassId: null */class E
}
