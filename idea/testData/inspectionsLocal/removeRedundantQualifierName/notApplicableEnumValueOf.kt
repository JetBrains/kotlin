// PROBLEM: none
// WITH_RUNTIME
package packageName

import packageName.MyEnum.*

enum class MyEnum {
    A, B, C, D, E;
}

fun main() {
    <caret>MyEnum.valueOf("A")
}