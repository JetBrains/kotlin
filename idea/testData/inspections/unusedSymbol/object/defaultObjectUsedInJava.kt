package companionObjectUsedInJava

import kotlin.platform.platformStatic

class A {
    companion object {
        val CONST = 42
    }
}

class B {
    companion object {
        platformStatic fun foo() {
        }
    }
}

class C {
    companion object Named {
        val CONST = 42
    }
}

class D {
    companion object Named {
        platformStatic fun foo() {
        }
    }
}
