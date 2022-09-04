package com.example

@Suppress("RedundantGetter", "RedundantSetter")
class SomeClass {

    var property_ChangedType: Long = 0
        get() = field
        set(value) {
            field = value
        }

    var property_ChangedGetterImpl: Int = 0
        get() {
            println("Getter implementation has changed!")
            return field
        }
        set(value) {
            field = value
        }

    var property_ChangedSetterImpl: Int = 0
        get() = field
        set(value) {
            println("Setter implementation has changed!")
            field = value
        }

    var property_Unchanged: Int = 0
        get() = field
        set(value) {
            field = value
        }

    private var privateProperty_ChangedType: Long = 0
        get() = field
        set(value) {
            field = value
        }
}

var inlineProperty_ChangedType_BackingField: Long = 0
var inlineProperty_ChangedGetterImpl_BackingField: Int = 0
var inlineProperty_ChangedSetterImpl_BackingField: Int = 0
var inlineProperty_Unchanged_BackingField: Int = 0
private var privateInlineProperty_ChangedType_BackingField: Long = 0

inline var inlineProperty_ChangedType: Long
    get() = inlineProperty_ChangedType_BackingField
    set(value) {
        inlineProperty_ChangedType_BackingField = value
    }

inline var inlineProperty_ChangedGetterImpl: Int
    get() {
        println("Getter implementation has changed!")
        return inlineProperty_ChangedGetterImpl_BackingField
    }
    set(value) {
        inlineProperty_ChangedGetterImpl_BackingField = value
    }

inline var inlineProperty_ChangedSetterImpl: Int
    get() = inlineProperty_ChangedSetterImpl_BackingField
    set(value) {
        println("Setter implementation has changed!")
        inlineProperty_ChangedSetterImpl_BackingField = value
    }

inline var inlineProperty_Unchanged: Int
    get() = inlineProperty_Unchanged_BackingField
    set(value) {
        inlineProperty_Unchanged_BackingField = value
    }

private inline var privateInlineProperty_ChangedType: Long
    get() = privateInlineProperty_ChangedType_BackingField
    set(value) {
        privateInlineProperty_ChangedType_BackingField = value
    }
