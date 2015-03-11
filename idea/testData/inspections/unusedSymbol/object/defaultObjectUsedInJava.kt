package defaultObjectUsedInJava

import kotlin.platform.platformStatic

class A {
    default object {
        val CONST = 42
    }
}

class B {
    default object {
        platformStatic fun foo() {
        }
    }
}

class C {
    default object Named {
        val CONST = 42
    }
}

class D {
    default object Named {
        platformStatic fun foo() {
        }
    }
}
