// LANGUAGE: +FullValueClasses
package test/* RootStructureElement */

abstract value class Base {/* ClassDeclarationStructureElement */
    abstract val size: Int/* DeclarationStructureElement */

    fun isEmpty(): Boolean = size == 0/* DeclarationStructureElement */
}

value class Point(val x: Int, val y: Int)/* DeclarationStructureElement */ : Base() {/* ClassDeclarationStructureElement */
    constructor(value: Int) : this(value, value)/* DeclarationStructureElement */

    init {/* DeclarationStructureElement */
        require(x >= 0)
    }

    override val size: Int/* DeclarationStructureElement */ get() = x + y

    fun sum(): Int {/* DeclarationStructureElement */
        return x + y
    }
}

value object Empty : Base() {/* ClassDeclarationStructureElement */
    override val size: Int/* DeclarationStructureElement */ get() = 0
}
