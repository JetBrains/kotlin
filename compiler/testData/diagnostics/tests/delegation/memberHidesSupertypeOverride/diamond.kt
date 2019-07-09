public interface Base {
    fun test() = "Base"
}

public interface Base2 : Base {
    override fun test() = "Base2"
}

public interface Base3 : Base {
    override fun test() = "Base3"
}

class Impl : Base

class Impl2 : Base2

class Impl3 : Base3

class ImplAll : Base, Base2, Base3 {
    override fun test(): String {
        return super<Base2>.test()
    }
}

fun box(): String {
    <!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>object<!> : Base2, Base3, Base by Impl() {

    }

    <!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>object<!> : Base2 by Impl2(), Base3, Base {

    }

    <!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>object<!> : Base2, Base3 by Impl3(), Base {

    }

    <!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE, DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE, DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE, MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>object<!> : Base2 by Impl2(), Base3 by Impl3(), Base by Impl() {

    }

    return "OK"
}
