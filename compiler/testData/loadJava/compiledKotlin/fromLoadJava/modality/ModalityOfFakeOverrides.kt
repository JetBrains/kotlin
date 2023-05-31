// TARGET_BACKEND: JVM
// FULL_JDK
// JAVAC_EXPECTED_FILE
// NO_CHECK_SOURCE_VS_BINARY

package test

import java.util.AbstractList

public open class ModalityOfFakeOverrides : AbstractList<String>() {
    override fun get(index: Int): String {
        return ""
    }

    override val size: Int get() = 0
}
