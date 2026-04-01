package test

import test.MyEnum.A

enum class MyEnum { A, B }

fun use(): MyEnum = <expr>test.MyEnum.A</expr>
