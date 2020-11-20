class A {
    protected interface AProtectedI {

    }

    interface APublicI {

    }
}

class B {
    protected class BProtected {

    }

    inner class BInner {

    }
}

private class C {
    class CPublic {

    }

    interface CPublicI {

    }
}

class D : A {
    class Test1 : A.AProtectedI {

    }
}

interface E {

}

class Test2 : A.APublicI, <!UNRESOLVED_REFERENCE!>B.BInner<!>() {

}

class Test3 : C.CPublicI, <!EXPOSED_SUPER_CLASS!>C<!> {

}

class Test4 : E, A.AProtectedI {

}

class Test5 : C.CPublicI, <!UNRESOLVED_REFERENCE!>B.BInner<!>() {

}

class Test6 : E, <!EXPOSED_SUPER_CLASS!>C.CPublic<!> {

}

class Test7 : <!UNRESOLVED_REFERENCE!>D.PublicButProtected<!> {

}