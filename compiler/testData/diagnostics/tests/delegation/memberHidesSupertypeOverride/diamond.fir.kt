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
    object : Base2, Base3, Base by Impl() {

    }

    object : Base2 by Impl2(), Base3, Base {

    }

    object : Base2, Base3 by Impl3(), Base {

    }

    object : Base2 by Impl2(), Base3 by Impl3(), Base by Impl() {

    }

    return "OK"
}
