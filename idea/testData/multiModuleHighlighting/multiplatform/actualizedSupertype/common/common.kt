package com.example

interface CommonMainIface {
    fun f() = Unit
}

internal expect abstract class CommonMainExpectDerivedClass constructor() : CommonMainIface

internal abstract class CommonMainImplClass : CommonMainExpectDerivedClass()