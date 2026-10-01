// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
import java.io.Serializable

value class Full(val x: Int, val y: String) : Serializable

value object Object : Serializable

@JvmInline
value class Inline(val s: String) : Serializable

value class WithWriteReplace(val x: Int) : Serializable {
    private fun writeReplace(): Any = x
}

@JvmRecord
value class Record(val x: Int) : Serializable

abstract value class Base : Serializable {
    protected fun writeReplace(): Any = this.toString()
}

value class InheritsWriteReplace(val x: Int) : Base()

abstract value class BaseWithoutWriteReplace : Serializable

value class InheritsSerializable(val x: Int) : BaseWithoutWriteReplace()

interface SerializableWithWriteReplace : Serializable {
    fun writeReplace(): Any = 0
}

value class InterfaceWriteReplace(val x: Int) : SerializableWithWriteReplace

value class RenamedWriteReplace(val x: Int) : Serializable {
    @JvmName("writeReplace")
    private fun replace(): Any = x
}

class Identity(val x: Int) : BaseWithoutWriteReplace()

value class NotSerializable(val x: Int)

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, objectDeclaration,
primaryConstructor, propertyDeclaration, stringLiteral, thisExpression, value */
