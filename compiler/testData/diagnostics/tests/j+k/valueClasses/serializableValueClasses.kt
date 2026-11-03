// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses +ContextParameters +CompanionBlocks
// TARGET_BACKEND: JVM
// JVM_TARGET: 28
// ENABLE_JVM_PREVIEW
// VALHALLA_VALUE_CLASSES
// WITH_STDLIB
// JDK_KIND: FULL_JDK_21
import java.io.Serializable

value <!SERIALIZABLE_VALUE_CLASS_WITHOUT_WRITE_REPLACE!>class Full<!>(val x: Int, val y: String) : Serializable

value <!SERIALIZABLE_VALUE_CLASS_WITHOUT_WRITE_REPLACE!>object Object<!> : Serializable

@JvmInline
value <!SERIALIZABLE_VALUE_CLASS_WITHOUT_WRITE_REPLACE!>class Inline<!>(val s: String) : Serializable

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

value <!SERIALIZABLE_VALUE_CLASS_WITHOUT_WRITE_REPLACE!>class InheritsSerializable<!>(val x: Int) : BaseWithoutWriteReplace()

interface SerializableWithWriteReplace : Serializable {
    fun writeReplace(): Any = 0
}

value <!SERIALIZABLE_VALUE_CLASS_WITHOUT_WRITE_REPLACE!>class InterfaceWriteReplace<!>(val x: Int) : SerializableWithWriteReplace

value class RenamedWriteReplace(val x: Int) : Serializable {
    @JvmName("writeReplace")
    private fun replace(): Any = x
}

class Identity(val x: Int) : BaseWithoutWriteReplace()

value class NotSerializable(val x: Int)

// The JDK uses a private `writeReplace` only of the serialized class itself.
abstract value class PrivateWriteReplaceBase : Serializable {
    private fun writeReplace(): Any = 0
}

value class InheritsPrivateWriteReplace(val x: Int) : PrivateWriteReplaceBase()

// An internal function has a mangled name on the JVM.
abstract value class InternalWriteReplaceBase : Serializable {
    internal fun writeReplace(): Any = 0
}

value class InheritsInternalWriteReplace(val x: Int) : InternalWriteReplaceBase()

// The JDK requires `writeReplace` to return `Object`.
value class WriteReplaceReturningString(val x: Int) : Serializable {
    private fun writeReplace(): String = ""
}

// A context parameter is a parameter of the JVM method.
value class WriteReplaceWithContextParameter(val x: Int) : Serializable {
    context(c: Int)
    private fun writeReplace(): Any = c
}

// A companion block member is a static JVM method.
value class CompanionBlockWriteReplace(val x: Int) : Serializable {
    companion {
        fun writeReplace(): Any = 0
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, integerLiteral,
interfaceDeclaration, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral, thisExpression, value */
