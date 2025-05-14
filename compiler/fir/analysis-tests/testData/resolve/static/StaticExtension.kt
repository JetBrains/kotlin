// RUN_PIPELINE_TILL: FRONTEND

class Foo {
    static fun ::Example.doubleStaticDispatch() {
        val x = staticScopeExtension()
    }
}

class Example {
    static fun simple(): String = "hello"

    static fun Foo.memberScopeExtension(): Int = 3
    static fun ::Foo.staticScopeExtension(): Boolean = true

    val member1 = Foo().memberScopeExtension()
    val member2 = Foo().<!UNRESOLVED_REFERENCE!>staticScopeExtension<!>()

    val static1 = Foo.staticScopeExtension()
    val static2 = Foo.<!UNRESOLVED_REFERENCE!>memberScopeExtension<!>()

    static fun staticFunction() {
        val x = Foo().memberScopeExtension()
        val y = Foo.staticScopeExtension()
    }

    fun memberFunction() {
        val x = Foo().memberScopeExtension()
        val y = Foo.staticScopeExtension()
    }
}

fun simpleUsage() {
    val x = Example.simple()
    val y = Example().<!UNRESOLVED_REFERENCE!>simple<!>()
}

fun ::Example.staticScopeExtensionToExample() {
    val x = Foo().memberScopeExtension()
    val y = Foo.staticScopeExtension()
}

fun ::Foo.staticScopeExtensionToFoo() {
    val y = Example.<!UNRESOLVED_REFERENCE!>staticScopeExtension<!>()
}

