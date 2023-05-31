// PLATFORM_DEPENDANT_METADATA
package test

import java.io.Serializable

public open class AllBoundsInWhen {
    public open fun <T> foo() where T: Serializable {
        throw UnsupportedOperationException()
    }
}
