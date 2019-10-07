/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

interface StubIrElement {
    fun <T, R> accept(visitor: StubIrVisitor<T, R>, data: T): R
}

sealed class StubContainer : StubIrElement {
    abstract val meta: StubContainerMeta
    abstract val classes: List<ClassStub>
    abstract val functions: List<FunctionalStub>
    abstract val properties: List<PropertyStub>
    abstract val typealiases: List<TypealiasStub>
    abstract val simpleContainers: List<SimpleStubContainer>
}

/**
 * Meta information about [StubContainer].
 * For example, can be used for comments in textual representation.
 */
class StubContainerMeta(
        val textAtStart: String = "",
        val textAtEnd: String = ""
)

class SimpleStubContainer(
        override val meta: StubContainerMeta = StubContainerMeta(),
        override val classes: List<ClassStub> = emptyList(),
        override val functions: List<FunctionalStub> = emptyList(),
        override val properties: List<PropertyStub> = emptyList(),
        override val typealiases: List<TypealiasStub> = emptyList(),
        override val simpleContainers: List<SimpleStubContainer> = emptyList()
) : StubContainer() {

    override fun <T, R> accept(visitor: StubIrVisitor<T, R>, data: T): R {
        return visitor.visitSimpleStubContainer(this, data)
    }
}

val StubContainer.children: List<StubIrElement>
    get() = (classes as List<StubIrElement>) + properties + functions + typealiases

/**
 * Marks that abstract value of such type can be passed as value.
 */
sealed class ValueStub

class TypeParameterStub(
        val name: String,
        val upperBound: StubType? = null
) {
    fun getStubType(nullable: Boolean) =
            TypeParameterType(name, nullable = nullable)

}

interface TypeArgument {
    object StarProjection : TypeArgument

    enum class Variance {
        INVARIANT,
        IN,
        OUT
    }
}

class TypeArgumentStub(
        val type: StubType,
        val variance: TypeArgument.Variance = TypeArgument.Variance.INVARIANT
) : TypeArgument

/**
 * Represents a source of StubIr element.
 */
sealed class StubOrigin {
    /**
     * Special case when element of IR was generated.
     */
    object None : StubOrigin()

    class ObjCMethod(
            val method: org.jetbrains.kotlin.native.interop.indexer.ObjCMethod,
            val container: ObjCContainer
    ) : StubOrigin()

    class ObjCClass(
            val clazz: org.jetbrains.kotlin.native.interop.indexer.ObjCClass
    ) : StubOrigin()

    class ObjCProtocol(
            val protocol: org.jetbrains.kotlin.native.interop.indexer.ObjCProtocol
    ) : StubOrigin()

    class Enum(val enum: EnumDef) : StubOrigin()

    class Function(val function: FunctionDecl) : StubOrigin()

    class FunctionParameter(val parameter: Parameter) : StubOrigin()

    class Struct(val struct: StructDecl) : StubOrigin()
}

interface StubElementWithOrigin : StubIrElement {
    val origin: StubOrigin
}

interface AnnotationHolder {
    val annotations: List<AnnotationStub>
}

sealed class AnnotationStub {
    sealed class ObjC : AnnotationStub() {
        object ConsumesReceiver : ObjC()
        object ReturnsRetained : ObjC()
        class Method(val selector: String, val encoding: String, val isStret: Boolean = false) : ObjC()
        class Factory(val selector: String, val encoding: String, val isStret: Boolean = false) : ObjC()
        object Consumed : ObjC()
        class Constructor(val selector: String, val designated: Boolean) : ObjC()
        class ExternalClass(val protocolGetter: String = "", val binaryName: String = "") : ObjC()
    }

    sealed class CCall : AnnotationStub() {
        object CString : CCall()
        object WCString : CCall()
        class Symbol(val symbolName: String) : CCall()
    }

    class CStruct(val struct: String) : AnnotationStub()
    class CNaturalStruct(val members: List<StructMember>) : AnnotationStub()

    class CLength(val length: Long) : AnnotationStub()

    class Deprecated(val message: String, val replaceWith: String) : AnnotationStub()
}

/**
 * Compile-time known values.
 */
sealed class ConstantStub : ValueStub()
class StringConstantStub(val value: String) : ConstantStub()
data class IntegralConstantStub(val value: Long, val size: Int, val isSigned: Boolean) : ConstantStub()
data class DoubleConstantStub(val value: Double, val size: Int) : ConstantStub()


class PropertyStub(
        val name: String,
        val type: StubType,
        val kind: Kind,
        val modality: MemberStubModality = MemberStubModality.FINAL,
        val receiverType: StubType? = null,
        override val annotations: List<AnnotationStub> = emptyList()
) : StubIrElement, AnnotationHolder {
    sealed class Kind {
        class Val(
                val getter: PropertyAccessor.Getter
        ) : Kind()

        class Var(
                val getter: PropertyAccessor.Getter,
                val setter: PropertyAccessor.Setter
        ) : Kind()

        class Constant(val constant: ConstantStub) : Kind()
    }

    override fun <T, R> accept(visitor: StubIrVisitor<T, R>, data: T): R {
        return visitor.visitProperty(this, data)
    }
}

enum class ClassStubModality {
    INTERFACE, OPEN, ABSTRACT, NONE
}

enum class VisibilityModifier {
    PRIVATE, PROTECTED, INTERNAL, PUBLIC
}

class ConstructorParameterStub(val name: String, val type: StubType, val qualifier: Qualifier = Qualifier.NONE) {
    sealed class Qualifier {
        class VAL(val overrides: Boolean) : Qualifier()
        class VAR(val overrides: Boolean) : Qualifier()
        object NONE : Qualifier()
    }
}

class GetConstructorParameter(
        val constructorParameterStub: ConstructorParameterStub
) : ValueStub()

class SuperClassInit(
        val type: StubType,
        val arguments: List<ValueStub> = listOf()
)

sealed class ClassStub : StubContainer(), StubElementWithOrigin, AnnotationHolder {

    abstract val superClassInit: SuperClassInit?
    abstract val interfaces: List<StubType>
    abstract val childrenClasses: List<ClassStub>
    abstract val companion : Companion?

    class Simple(
            val classifier: Classifier,
            val modality: ClassStubModality,
            val constructorParameters: List<ConstructorParameterStub> = emptyList(),
            override val superClassInit: SuperClassInit? = null,
            override val interfaces: List<StubType> = emptyList(),
            override val properties: List<PropertyStub> = emptyList(),
            override val origin: StubOrigin,
            override val annotations: List<AnnotationStub> = emptyList(),
            override val childrenClasses: List<ClassStub> = emptyList(),
            override val companion: Companion? = null,
            override val functions: List<FunctionalStub> = emptyList(),
            override val simpleContainers: List<SimpleStubContainer> = emptyList()
    ) : ClassStub()

    class Companion(
            override val superClassInit: SuperClassInit? = null,
            override val interfaces: List<StubType> = emptyList(),
            override val properties: List<PropertyStub> = emptyList(),
            override val origin: StubOrigin = StubOrigin.None,
            override val annotations: List<AnnotationStub> = emptyList(),
            override val childrenClasses: List<ClassStub> = emptyList(),
            override val functions: List<FunctionalStub> = emptyList(),
            override val simpleContainers: List<SimpleStubContainer> = emptyList()
    ) : ClassStub() {
        override val companion: Companion? = null
    }

    class Enum(
            val classifier: Classifier,
            val entries: List<EnumEntryStub>,
            val constructorParameters: List<ConstructorParameterStub> = emptyList(),
            override val superClassInit: SuperClassInit? = null,
            override val interfaces: List<StubType> = emptyList(),
            override val properties: List<PropertyStub> = emptyList(),
            override val origin: StubOrigin,
            override val annotations: List<AnnotationStub> = emptyList(),
            override val childrenClasses: List<ClassStub> = emptyList(),
            override val companion: Companion?= null,
            override val functions: List<FunctionalStub> = emptyList(),
            override val simpleContainers: List<SimpleStubContainer> = emptyList()
    ) : ClassStub()

    override val meta: StubContainerMeta = StubContainerMeta()

    override val classes: List<ClassStub>
        get() = childrenClasses + listOfNotNull(companion)

    override fun <T, R> accept(visitor: StubIrVisitor<T, R>, data: T) =
        visitor.visitClass(this, data)

    override val typealiases: List<TypealiasStub> = emptyList()
}

class ReceiverParameterStub(
        val type: StubType
)

class FunctionParameterStub(
        val name: String,
        val type: StubType,
        override val annotations: List<AnnotationStub> = emptyList(),
        val isVararg: Boolean = false,
        val origin: StubOrigin = StubOrigin.None
) : AnnotationHolder

enum class MemberStubModality {
    OVERRIDE,
    OPEN,
    FINAL
}

interface FunctionalStub : AnnotationHolder, StubIrElement, NativeBacked {
    val parameters: List<FunctionParameterStub>
}

sealed class PropertyAccessor : FunctionalStub {

    sealed class Getter : PropertyAccessor() {

        override val parameters: List<FunctionParameterStub> = emptyList()

        class SimpleGetter(
                override val annotations: List<AnnotationStub> = emptyList(),
                val constant: ConstantStub? = null
        ) : Getter()

        class ExternalGetter(
                override val annotations: List<AnnotationStub> = emptyList()
        ) : Getter()

        class ArrayMemberAt(
                val offset: Long
        ) : Getter() {
            override val parameters: List<FunctionParameterStub> = emptyList()
            override val annotations: List<AnnotationStub> = emptyList()
        }

        class MemberAt(
                val offset: Long,
                val typeArguments: List<TypeArgumentStub> = emptyList(),
                val hasValueAccessor: Boolean
        ) : Getter() {
            override val annotations: List<AnnotationStub> = emptyList()
        }

        class ReadBits(
                val offset: Long,
                val size: Int,
                val signed: Boolean
        ) : Getter() {
            override val annotations: List<AnnotationStub> = emptyList()
        }

        class InterpretPointed(val cGlobalName:String, pointedType: StubType) : Getter() {
            override val annotations: List<AnnotationStub> = emptyList()
            val typeParameters: List<StubType> = listOf(pointedType)
        }
    }

    sealed class Setter : PropertyAccessor() {

        override val parameters: List<FunctionParameterStub> = emptyList()

        class SimpleSetter(
                override val annotations: List<AnnotationStub> = emptyList()
        ) : Setter()

        class ExternalSetter(
                override val annotations: List<AnnotationStub> = emptyList()
        ) : Setter()

        class MemberAt(
                val offset: Long,
                override val annotations: List<AnnotationStub> = emptyList(),
                val typeArguments: List<TypeArgumentStub> = emptyList()
        ) : Setter()

        class WriteBits(
                val offset: Long,
                val size: Int,
                override val annotations: List<AnnotationStub> = emptyList()
        ) : Setter()
    }

    override fun <T, R> accept(visitor: StubIrVisitor<T, R>, data: T) =
        visitor.visitPropertyAccessor(this, data)

}

class FunctionStub(
        val name: String,
        val returnType: StubType,
        override val parameters: List<FunctionParameterStub>,
        override val origin: StubOrigin,
        override val annotations: List<AnnotationStub>,
        val external: Boolean = false,
        val receiver: ReceiverParameterStub?,
        val modality: MemberStubModality,
        val typeParameters: List<TypeParameterStub> = emptyList()
) : StubElementWithOrigin, FunctionalStub {

    override fun <T, R> accept(visitor: StubIrVisitor<T, R>, data: T) =
        visitor.visitFunction(this, data)
}

// TODO: should we support non-trivial constructors?
class ConstructorStub(
        override val parameters: List<FunctionParameterStub>,
        override val annotations: List<AnnotationStub>,
        val visibility: VisibilityModifier = VisibilityModifier.PUBLIC
) : FunctionalStub {

    override fun <T, R> accept(visitor: StubIrVisitor<T, R>, data: T) =
        visitor.visitConstructor(this, data)
}

class EnumEntryStub(
        val name: String,
        val constant: IntegralConstantStub,
        val aliases: List<Alias>
) {
    class Alias(val name: String)
}

class TypealiasStub(
        val alias: Classifier,
        val aliasee: StubType
) : StubIrElement {

    override fun <T, R> accept(visitor: StubIrVisitor<T, R>, data: T) =
        visitor.visitTypealias(this, data)
}