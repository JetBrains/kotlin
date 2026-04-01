// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// FILE: kt46408.kt

open class User<IT : Identity> {
    protected fun processIdentity(identity: IT) {
        identity.ok = "OK"
    }
}

class UserAc : User<AcIdentity>() {
    fun doStuff(data: Container) {
        data.processEachWith(this::processIdentity)
    }
}

interface Identity {
    var ok: String
}

class AcIdentity(override var ok: String) : Identity

class Container {
    var id = AcIdentity("xxx")

    fun processEachWith(action: Action<AcIdentity>) {
        action.execute(id)
    }
}

fun box(): String {
    val c = Container()
    UserAc().doStuff(c)
    return c.id.ok
}

// FILE: Action.java

public interface Action<T> {
    void execute(T var1);
}