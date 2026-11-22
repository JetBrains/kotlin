package foo

import test.A.CompanionObject.CONSTANT_VALUE

class B {
    companion object {
        fun main() {
            println("Import companion constant: ${CONSTANT_VALUE}")
        }

    }

}