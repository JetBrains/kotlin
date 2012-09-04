package test

import java.util.AbstractList

public open class ModalityOfFakeOverrides : AbstractList<String>() {
    override fun get(p0: Int): String {
        return ""
    }

    override fun size(): Int = 0
}
