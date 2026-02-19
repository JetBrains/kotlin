package test

class MyClass

typealias MyTypeAliasFirst = MyClass

typealias MyTypeAliasSecond = MyTypeAliasFirst

fun usage() {
    <caret>MyTypeAliasSecond()
}