package companions

import kotlin.platform.platformName
import kotlin.platform.platformStatic

class A {
    companion object {
        platformStatic
        fun main(args: Array<String>) {
            // yes
        }
    }
}

class B {
    companion object {
        fun main(args: Array<String>) {
            // no
        }
    }
}

class C {
    companion object {
        platformStatic
        platformName("main0")
        fun main(args: Array<String>) { // no
        }
    }
}

class D {
    companion object {
        platformStatic
        platformName("main")
        fun badName(args: Array<String>) { // yes
        }
    }
}
