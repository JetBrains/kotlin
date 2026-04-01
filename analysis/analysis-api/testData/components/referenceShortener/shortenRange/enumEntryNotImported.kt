// FILE: MyEnum.kt
package test

enum class MyEnum { A, B }

// FILE: usage.kt
package usage

import test.MyEnum

fun use(): MyEnum = <expr>test.MyEnum.A</expr>
