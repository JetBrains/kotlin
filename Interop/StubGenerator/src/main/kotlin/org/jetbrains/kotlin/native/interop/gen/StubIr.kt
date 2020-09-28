/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

// TODO: Replace all usages of these strings with constants.
const val cinteropPackage = "kotlinx.cinterop"
const val cinteropInternalPackage = "$cinteropPackage.internal"

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
            TypeParameterType(name, nullable = nullable, typeParameterDeclaration = this)

}

interface TypeArgument {
    object StarProjection : TypeArgument {
        override fun toString(): String =
                "*"
    }

    enum class Variance {
        INVARIANT,
        IN,
        OUT
    }
}

class TypeArgumentStub(
        val type: StubType,
        val variance: TypeArgument.Variance = TypeArgument.Variance.INVARIANT
) : TypeArgument {
    override fun toString(): String =
            type.toString()
}

/**
 * Represents a source of StubIr element.
 */
sealed class StubOrigin {
    /**
     * Special case when element of IR was generated.
     */
    sealed class Synthetic : StubOrigin() {
        object CompanionObject : Synthetic()

        /**
         * Denotes default constructor that was generated and has no real origin.
         */
        object DefaultConstructor : Synthetic()

        /**
         * CEnum.Companion.byValue.
         */
        class EnumByValue(val enum: EnumDef) : Synthetic()

        /**
         * CEnum.value.
         */
        class EnumValueField(val enum: EnumDef) : Synthetic()

        /**
         * E.CEnumVar.value.
         */
        class EnumVarValueField(val enum: EnumDef) : Synthetic()
    }

    class ObjCCategoryInitMethod(
            val method: org.jetbrains.kotlin.native.interop.indexer.ObjCMethod
    ) : StubOrigin()

    class ObjCMethod(
            val method: org.jetbrains.kotlin.native.interop.indexer.ObjCMethod,
            val container: ObjCContainer
    ) : StubOrigin()

    class ObjCProperty(
            val property: org.jetbrains.kotlin.native.interop.indexer.ObjCProperty,
            val container: ObjCContainer
    ) : StubOrigin()

    class ObjCClass(
            val clazz: org.jetbrains.kotlin.native.interop.indexer.ObjCClass,
            val isMeta: Boolean
    ) : StubOrigin()

    class ObjCProtocol(
            val protocol: org.jetbrains.kotlin.native.interop.indexer.ObjCProtocol,
            val isMeta: Boolean
    ) : StubOrigin()

    class Enum(val enum: EnumDef) : StubOrigin()

    class EnumEntry(val constant: EnumConstant) : StubOrigin()

    class Function(val function: FunctionDecl) : StubOrigin()

    class Struct(val struct: StructDecl) : StubOrigin()

    class StructMember(
            val member: org.jetbrains.kotlin.native.interop.indexer.StructMember
    ) : StubOrigin()

    class Constant(val constantDef: ConstantDef): StubOrigin()

    class Global(val global: GlobalDecl) : StubOrigin()

    class TypeDef(val typedefDef: TypedefDef) : StubOrigin()

    class VarOf(val typeOrigin: StubOrigin) : StubOrigin()
}

interface StubElementWithOrigin : StubIrElement {
    val origin: StubOrigin
}

interface AnnotationHolder {
    val annotations: List<AnnotationStub>
}

sealed class AnnotationStub(val classifier: Classifier) {

    sealed class ObjC(classifier: Classifier) : AnnotationStub(classifier) {
        object ConsumesReceiver :
                ObjC(cCallClassifier.nested("ConsumesReceiver"))

        object ReturnsRetained :
                ObjC(cCallClassifier.nested("ReturnsRetained"))

        class Method(val selector: String, val encoding: String, val isStret: Boolean = false) :
                ObjC(Classifier.topLevel(cinteropPackage, "ObjCMethod"))

        class Factory(val selector: String, val encoding: String, val isStret: Boolean = false) :
                ObjC(Classifier.topLevel(cinteropPackage, "ObjCFactory"))

        object Consumed :
                ObjC(cCallClassifier.nested("Consumed"))

        class Constructor(val selector: String, val designated: Boolean) :
                ObjC(Classifier.topLevel(cinteropPackage, "ObjCConstructor"))

        class ExternalClass(val protocolGetter: String = "", val binaryName: String = "") :
                ObjC(Classifier.topLevel(cinteropPackage, "ExternalObjCClass"))
    }

    sealed class CCall(classifier: Classifier) : AnnotationStub(classifier) {
        object CString : CCall(cCallClassifier.nested("CString"))
        object WCString : CCall(cCallClassifier.nested("WCString"))
        class Symbol(val symbolName: String) : CCall(cCallClassifier)
    }

    class CStruct(val struct: String) : AnnotationStub(cStructClassifier) {
        class MemberAt(val offset: Long) : AnnotationStub(cStructClassifier.nested("MemberAt"))

        class ArrayMemberAt(val offset: Long) : AnnotationStub(cStructClassifier.nested("ArrayMemberAt"))

        class BitField(val offset: Long, val size: Int) : AnnotationStub(cStructClassifier.nested("BitField"))

        class VarType(val size: Long, val align: Int) : AnnotationStub(cStructClassifier.nested("VarType"))
    }

    class CNaturalStruct(val members: List<StructMember>) :
            AnnotationStub(Classifier.topLevel(cinteropPackage, "CNaturalStruct"))

    class CLength(val length: Long) :
            AnnotationStub(Classifier.topLevel(cinteropPackage, "CLength"))

    class Deprecated(val message: String, val replaceWith: String, val level: DeprecationLevel) :
            AnnotationStub(Classifier.topLevel("kotlin", "Deprecated")) {
        companion object {
            val unableToImport = Deprecated(
                    "Unable to import this declaration",
                    "",
                    DeprecationLevel.ERROR
            )

            val deprecatedCVariableCompanion = Deprecated(
                    "Use sizeOf<T>() or alignOf<T>() instead.",
                    "",
                    DeprecationLevel.WARNING
            )

            val deprecatedCEnumByValue = Deprecated(
                    "Will be removed.",
                    "",
                    DeprecationLevel.WARNING
            )
        }
    }


    class CEnumEntryAlias(val entryName: String) :
            AnnotationStub(Classifier.topLevel(cinteropInternalPackage, "CEnumEntryAlias"))

    class CEnumVarTypeSize(val size: Int) :
            AnnotationStub(Classifier.topLevel(cinteropInternalPackage, "CEnumVarTypeSize"))

    private companion object {
        val cCallClassifier = Classifier.topLevel(cinteropInternalPackage, "CCall")

        val cStructClassifier = Classifier.topLevel(cinteropInternalPackage, "CStruct")
    }
}

/**
 * Compile-time known values.
 */
sealed class ConstantStub : ValueStub()
class StringConstantStub(val value: String) : ConstantStub()
data class IntegralConstantStub(val value: Long, val size: Int, val isSigned: Boolean) : ConstantStub()
data class DoubleConstantStub(val value: Double, val size: Int) : ConstantStub()


data class PropertyStub(
        val name: String,
        val type: StubType,
        val kind: Kind,
        val modality: MemberStubModality = MemberStubModality.FINAL,
        val receiverType: StubType? = null,
        override val annotations: List<AnnotationStub> = emptyList(),
        val origin: StubOrigin,
        val isOverride: Boolean = false
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

class GetConstructorParameter(
        val constructorParameterStub: FunctionParameterStub
) : ValueStub()

class SuperClassInit(
        val type: StubType,
        val arguments: List<ValueStub> = listOf()
)

// TODO: Consider unifying these classes.
sealed class ClassStub : StubContainer(), StubElementWithOrigin, AnnotationHolder {

    abstract val superClassInit: SuperClassInit?
    abstract val interfaces: List<StubType>
    abstract val childrenClasses: List<ClassStub>
    abstract val companion : Companion?
    abstract val classifier: Classifier

    class Simple(
            override val classifier: Classifier,
            val modality: ClassStubModality,
            constructors: List<ConstructorStub> = emptyList(),
            methods: List<FunctionStub> = emptyList(),
            override val superClassInit: SuperClassInit? = null,
            override val interfaces: List<StubType> = emptyList(),
            override val properties: List<PropertyStub> = emptyList(),
            override val origin: StubOrigin,
            override val annotations: List<AnnotationStub> = emptyList(),
            override val childrenClasses: List<ClassStub> = emptyList(),
            override val companion: Companion? = null,
            override val simpleContainers: List<SimpleStubContainer> = emptyList()
    ) : ClassStub() {
        override val functions: List<FunctionalStub> = constructors + methods
    }

    class Companion(
            override val classifier: Classifier,
            methods: List<FunctionStub> = emptyList(),
            override val superClassInit: SuperClassInit? = null,
            override val interfaces: List<StubType> = emptyList(),
            override val properties: List<PropertyStub> = emptyList(),
            override val origin: StubOrigin = StubOrigin.Synthetic.CompanionObject,
            override val annotations: List<AnnotationStub> = emptyList(),
            override val childrenClasses: List<ClassStub> = emptyList(),
            override val simpleContainers: List<SimpleStubContainer> = emptyList()
    ) : ClassStub() {
        override val companion: Companion? = null

        override val functions: List<FunctionalStub> = methods
    }

    class Enum(
            override val classifier: Classifier,
            val entries: List<EnumEntryStub>,
            constructors: List<ConstructorStub>,
            override val superClassInit: SuperClassInit? = null,
            override val interfaces: List<StubType> = emptyList(),
            override val properties: List<PropertyStub> = emptyList(),
            override val origin: StubOrigin,
            override val annotations: List<AnnotationStub> = emptyList(),
            override val childrenClasses: List<ClassStub> = emptyList(),
            override val companion: Companion?= null,
            override val simpleContainers: List<SimpleStubContainer> = emptyList()
    ) : ClassStub() {
        override val functions: List<FunctionalStub> = constructors
    }

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
        val isVararg: Boolean = false
) : AnnotationHolder

enum class MemberStubModality {
    OPEN,
    FINAL,
    ABSTRACT
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

        class GetConstructorParameter(
                val constructorParameter: FunctionParameterStub,
                override val annotations: List<AnnotationStub> = emptyList()
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

        class GetEnumEntry(
                val enumEntryStub: EnumEntryStub,
                override val annotations: List<AnnotationStub> = emptyList()
        ) : Getter()
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

data class FunctionStub(
        val name: String,
        val returnType: StubType,
        override val parameters: List<FunctionParameterStub>,
        override val origin: StubOrigin,
        override val annotations: List<AnnotationStub>,
        val external: Boolean = false,
        val receiver: ReceiverParameterStub?,
        val modality: MemberStubModality,
        val typeParameters: List<TypeParameterStub> = emptyList(),
        val isOverride: Boolean = false
) : StubElementWithOrigin, FunctionalStub {

    override fun <T, R> accept(visitor: StubIrVisitor<T, R>, data: T) =
        visitor.visitFunction(this, data)
}

// TODO: should we support non-trivial constructors?
class ConstructorStub(
        override val parameters: List<FunctionParameterStub> = emptyList(),
        override val annotations: List<AnnotationStub> = emptyList(),
        val isPrimary: Boolean,
        val visibility: VisibilityModifier = VisibilityModifier.PUBLIC,
        val origin: StubOrigin
) : FunctionalStub {

    override fun <T, R> accept(visitor: StubIrVisitor<T, R>, data: T) =
        visitor.visitConstructor(this, data)
}

class EnumEntryStub(
        val name: String,
        val constant: IntegralConstantStub,
        val origin: StubOrigin.EnumEntry,
        val ordinal: Int
)

class TypealiasStub(
        val alias: Classifier,
        val aliasee: StubType,
        val origin: StubOrigin
) : StubIrElement {

    override fun <T, R> accept(visitor: StubIrVisitor<T, R>, data: T) =
        visitor.visitTypealias(this, data)
}