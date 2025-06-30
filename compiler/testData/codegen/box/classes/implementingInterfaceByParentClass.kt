// KT-77148
// FILE: foo.kt
package com.example

interface SimpleRouter<ROUTE> {
    val currentRoute: ROUTE

    // `navigate_rnj4s8_k$` is the original (generic) function. We generate exactly one bridge from where it's called.
    // FUNCTION_CALLED_TIMES: navigate_rnj4s8_k$ count=1
    fun navigate(route: ROUTE)
}

interface RouterWithUrl<ROUTE : Any> : SimpleRouter<ROUTE>

open class SimpleRouterImpl<ROUTE : Any>(
    initialRoute: ROUTE,
) : SimpleRouter<ROUTE> {
    override var currentRoute: ROUTE = initialRoute
    override fun navigate(route: ROUTE) { currentRoute = route }
}

class RouterWithUrlImpl<ROUTE : Any>(
    initialRoute: ROUTE,
) : SimpleRouterImpl<ROUTE>(initialRoute = initialRoute), RouterWithUrl<ROUTE>

class RouterWithUrlFactory<ROUTE : Any>(private val initialRoute: ROUTE) {
    fun create(): RouterWithUrl<ROUTE> = RouterWithUrlImpl(initialRoute)
}

fun box(): String {
    val router = RouterWithUrlFactory("/").create()

    router.navigate("OK")

    return router.currentRoute
}
