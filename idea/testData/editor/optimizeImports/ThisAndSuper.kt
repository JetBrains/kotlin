package a

import java.util.ArrayList

class A : b.`super`() {
    fun f() {
        this.f()
    }

    override fun getUsableSpace(): Long {
        return super.getUsableSpace()
    }

    companion object {
        fun g() {
            this.g()
        }
    }
}