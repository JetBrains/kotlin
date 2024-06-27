package test.pkg

import java.io.Closeable

internal class CloseMe() : Closeable {
    override fun close() {
    }
}

internal fun makeCloseMe(): CloseMe = CloseMe()

fun interface ResourceFactory {
    fun getResource(): Closeable
}

fun consumeCloseable(factory: ResourceFactory) = factory.getResource().use {  }

fun testReturnToImplicitLambdaLabel() {
    consumeCloseable {
        return@consumeCloseable makeCloseMe()
    }
}

fun testReturnToExplicitLambdaLabel() {
    consumeCloseable label@{<caret>
        return@label makeCloseMe()
    }
}