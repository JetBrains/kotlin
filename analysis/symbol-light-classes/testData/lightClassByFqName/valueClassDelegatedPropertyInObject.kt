// one.DefaultApi
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

@JvmInline
value class MyData(val value: Int)

interface Api {
    val x: MyData
    var y: MyData
}

object DefaultApi {
    object Base : Api {
        override val x: MyData = MyData(5)
        override var y: MyData = MyData(10)
    }

    object Derived : Api by Base
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: DefaultApi.class[x;y]
// LIGHT_ELEMENTS_NO_DECLARATION: DefaultApi.class[DefaultApi;INSTANCE;getX-yn2Urv4;getX-yn2Urv4;getY-yn2Urv4;getY-yn2Urv4;setY-5CKMQKQ;setY-5CKMQKQ]
