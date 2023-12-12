// FIR_DUMP

class CompanionOnly {
    @<!UNRESOLVED_REFERENCE!>Ann<!>
    companion object {
        annotation class Ann

        @Ann
        object Foo
    }
}

class Test {
    annotation class Ann

    @Ann
    companion object {
        annotation class Ann

        @Ann
        object Foo
    }
}

open class Super {
    annotation class Ann
}

class TestWithSuperAndOwn : Super() {
    annotation class Ann

    @Ann
    companion object {
        annotation class Ann

        @Ann
        object Foo
    }
}

class TestWithSuperOnly : Super() {
    @Ann // Change in resolution from K1 to K2, see KT-64299
    companion object {
        annotation class Ann

        @Ann
        object Foo
    }
}

open class SuperWithCompanion {
    companion object {
        annotation class Ann
    }
}


class TestWithSuperWithCompanionOnly : SuperWithCompanion() {
    @<!UNRESOLVED_REFERENCE!>Ann<!>
    companion object {
        annotation class Ann

        @Ann
        object Foo
    }
}

class TestWithSuperWithCompanionOnly2 : SuperWithCompanion() {
    @<!UNRESOLVED_REFERENCE!>Ann<!>
    companion object {
        @<!UNRESOLVED_REFERENCE!>Ann<!>
        object Foo
    }
}