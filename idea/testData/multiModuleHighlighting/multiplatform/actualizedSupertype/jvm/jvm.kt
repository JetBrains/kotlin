package com.example

internal actual abstract class CommonMainExpectDerivedClass actual constructor() : CommonMainIface

internal class IosMainImplClass : CommonMainImplClass()

internal fun getInstance() = IosMainImplClass()

fun main() {
    getInstance().f()
    (getInstance() as CommonMainImplClass).f()
    (getInstance() as CommonMainExpectDerivedClass).f()
    (getInstance() as CommonMainIface).f()
}