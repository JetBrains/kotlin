package test

import java.util.AbstractList

public open class ModalityOfFakeOverrides : AbstractList<String>() {
    override fun get(index: Int): String {
        return ""
    }

    override fun size(): Int = 0
}
