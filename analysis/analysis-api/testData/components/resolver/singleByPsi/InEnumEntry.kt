// DISABLE_DEPENDED_MODE
package test

val TOP_LEVEL = 5

enum class MyEnum(value: Int) {
    VALUE(<caret>TOP_LEVEL)
}
