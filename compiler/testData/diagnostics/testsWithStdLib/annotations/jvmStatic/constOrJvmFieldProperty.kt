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
