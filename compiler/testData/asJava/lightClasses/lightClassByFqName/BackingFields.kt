// one.BackingFields
package one;

class BackingFields {
    val withoutBackingFieldPropertyWithLocalDeclaration: Int
        get() {
            val field = 1
            return field
        }

    val withoutBackingFieldPropertyWithNestedLocalDeclaration: Int
        get() {
            return run {
                val field = 2
                field
            }
        }

    val withoutBackingFieldPropertyWithNestedLocalDeclarationAsExpressionBody: Int
        get() = run {
            val field = 3
            field
        }

    val withoutBackingFieldPropertyWithOuterLocalDeclaration: Int
        get() {
            val field = 4
            return run {
                field
            }
        }

    val withBackingFieldPropertyWithLocalDeclaration: Int = 5
        get() {
            field
            val field = 6
            return field
        }

    val withBackingFieldPropertyWithNestedLocalDeclaration: Int = 7
        get() {
            run {
                val field = 8
                field
            }

            return field
        }

    val withBackingFieldProperty: Int = 9

    val withBackingFieldPropertyWithDummyGetter: Int = 10
        get

    var withBackingFieldVariableWithDummyGetterAndSetter: Int = 11
        get
        set

    var withBackingFieldVariableWithDummyGetter: Int = 12
        get

    var withBackingFieldVariableWithDummySetter: Int = 13
        get

    var withoutBackingFieldVariableWithLocalDeclarationInsideSetter: Int
        get() = 14
        set(value) {
            val field = 15
            field
        }

    var withoutBackingFieldVariableWithNestedLocalDeclarationInsideSetter: Int
        get() = 16
        set(value) {
            run {
                val field = 17
                field
            }
        }

    var withoutBackingFieldVariableWithNestedLocalDeclarationInsideSetterAsExpressionBody: Int
        get() = 18
        set(value) = run {
            val field = 19
            field
            Unit
        }

    var withoutBackingFieldVariableWithOuterLocalDeclarationInsideSetter: Int
        get() = 20
        set(value) {
            val field = 21
            run {
                field
            }
        }

    var withoutBackingFieldVariableWithLocalDeclarationInsideGetter: Int
        get() {
            val field = 22
            field
            return field
        }
        set(value) {

        }

    var withoutBackingFieldVariableWithNestedLocalDeclarationInsideGetter: Int
        get() {
            run {
                val field = 23
                field
            }
            return 24
        }
        set(value) {
        }

    var withoutBackingFieldVariableWithNestedLocalDeclarationInsideGetterAsExpressionBody: Int
        get() = run {
            val field = 25
            field
        }
        set(value) {
        }

    var withoutBackingFieldVariableWithOuterLocalDeclarationInsideGetter: Int
        get() {
            val field = 26
            run {
                field
            }

            return field
        }
        set(value) {

        }
}