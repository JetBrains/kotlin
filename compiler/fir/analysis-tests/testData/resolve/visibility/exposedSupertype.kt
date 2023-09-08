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

class D : <!FINAL_SUPERTYPE, SUPERTYPE_NOT_INITIALIZED!>A<!> {
    class Test1 : A.AProtectedI {

    }
}

interface E {

}

class Test2 : A.APublicI, <!FINAL_SUPERTYPE, UNRESOLVED_REFERENCE!>B.BInner<!>() {

}

class Test3 : C.CPublicI, <!EXPOSED_SUPER_CLASS, FINAL_SUPERTYPE, SUPERTYPE_NOT_INITIALIZED!>C<!> {

}

class Test4 : E, A.<!INVISIBLE_REFERENCE!>AProtectedI<!> {

}

class Test5 : C.CPublicI, <!FINAL_SUPERTYPE, UNRESOLVED_REFERENCE!>B.BInner<!>() {

}

class Test6 : E, <!EXPOSED_SUPER_CLASS, FINAL_SUPERTYPE, SUPERTYPE_NOT_INITIALIZED!>C.CPublic<!> {

}

class Test7 : D.<!UNRESOLVED_REFERENCE!>PublicButProtected<!> {

}
