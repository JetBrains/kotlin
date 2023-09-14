// FIR_IDENTICAL
class A {

    companion object {

        <!JVM_STATIC_ON_CONST_OR_JVM_FIELD!>@JvmStatic const val z<!> = 1;

        <!JVM_STATIC_ON_CONST_OR_JVM_FIELD!>@JvmStatic @JvmField val x<!> = 1;
    }

}


object B {

    <!JVM_STATIC_ON_CONST_OR_JVM_FIELD!>@JvmStatic const val z<!> = 1;

    <!JVM_STATIC_ON_CONST_OR_JVM_FIELD!>@JvmStatic @JvmField val x<!> = 1;
}

typealias TAStatic = JvmStatic

object C {
    <!JVM_STATIC_ON_CONST_OR_JVM_FIELD!>@TAStatic const val z<!> = 1;

    <!JVM_STATIC_ON_CONST_OR_JVM_FIELD!>@TAStatic @JvmField val x<!> = 1;
}
