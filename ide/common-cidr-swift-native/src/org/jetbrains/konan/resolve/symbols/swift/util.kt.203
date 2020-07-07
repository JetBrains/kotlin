package org.jetbrains.konan.resolve.symbols.swift

import com.jetbrains.swift.codeinsight.SwiftCommonClassNames
import com.jetbrains.swift.psi.impl.types.SwiftFunctionDomainTypeImpl
import com.jetbrains.swift.psi.types.SwiftClassType
import com.jetbrains.swift.psi.types.SwiftContext
import com.jetbrains.swift.psi.types.SwiftType
import com.jetbrains.swift.psi.types.SwiftTypeFactory
import com.jetbrains.swift.symbols.*
import com.jetbrains.swift.symbols.impl.SwiftAttributesInfoImpl
import com.jetbrains.swift.symbols.swiftoc.SwiftGlobalApiNotes
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.utils.addToStdlib.indexOfOrNull
import java.util.*

private val typeFactory: SwiftTypeFactory
    get() = SwiftTypeFactory.getInstance()

fun ObjCType.convertType(context: SwiftSymbol): SwiftType = TypeConverter(context).convert(this)

private class TypeConverter(
    private val symbol: SwiftSymbol
) {
    private val context: SwiftContext = SwiftContext.of(symbol)
    private val psiContext = symbol.getContainingPsiFile(symbol.project)

    private val globalApiNotes = SwiftGlobalApiNotes.getInstance()

    fun convert(type: ObjCType): SwiftType {
        return when (type) {
            is ObjCRawType ->
                SwiftType.UNKNOWN //todo [medvedev]
            is ObjCClassType -> classType(convertReference(type.className, SwiftGlobalApiNotes.Kind.Other))
            is ObjCGenericTypeDeclaration ->
                SwiftType.UNKNOWN //todo [medvedev]
            is ObjCProtocolType -> classType(convertReference(type.protocolName, SwiftGlobalApiNotes.Kind.Protocol))
            is ObjCIdType -> SwiftType.ANY
            is ObjCInstanceType -> selfType()
            is ObjCBlockPointerType -> functionType(type)
            is ObjCMetaClassType -> classType("AnyClass")
            is ObjCNullableReferenceType -> optionalType(convert(type.nonNullType))
            is ObjCPointerType ->
                SwiftType.UNKNOWN //todo [medvedev]
            is ObjCVoidType -> SwiftType.VOID
            is ObjCPrimitiveType.NSUInteger -> classType("Swift.UInt")
            is ObjCPrimitiveType.BOOL -> classType(SwiftCommonClassNames.swiftBool)
            is ObjCPrimitiveType.unichar -> classType("Foundation.unichar")
            is ObjCPrimitiveType.int8_t -> classType("Swift.Int8")
            is ObjCPrimitiveType.int16_t -> classType("Swift.Int16")
            is ObjCPrimitiveType.int32_t -> classType("Swift.Int32")
            is ObjCPrimitiveType.int64_t -> classType("Swift.Int64")
            is ObjCPrimitiveType.uint8_t -> classType("Swift.UInt8")
            is ObjCPrimitiveType.uint16_t -> classType("Swift.UInt16")
            is ObjCPrimitiveType.uint32_t -> classType("Swift.UInt32")
            is ObjCPrimitiveType.uint64_t -> classType("Swift.UInt64")
            is ObjCPrimitiveType.float -> classType(SwiftCommonClassNames.swiftFloat)
            is ObjCPrimitiveType.double -> classType(SwiftCommonClassNames.swiftDouble)
            is ObjCPrimitiveType.NSInteger -> classType(SwiftCommonClassNames.swiftInt)
            is ObjCPrimitiveType.char -> classType("Swift.Int8")
            is ObjCPrimitiveType.unsigned_char -> classType("Swift.UInt8")
            is ObjCPrimitiveType.unsigned_short -> classType("Swift.UInt16")
            is ObjCPrimitiveType.int -> classType("Swift.Int32")
            is ObjCPrimitiveType.unsigned_int -> classType("Swift.UInt32")
            is ObjCPrimitiveType.long -> classType("Swift.Int")
            is ObjCPrimitiveType.unsigned_long -> classType("Swift.UInt")
            is ObjCPrimitiveType.long_long -> classType("Swift.Int64")
            is ObjCPrimitiveType.unsigned_long_long -> classType("Swift.UInt64")
            is ObjCPrimitiveType.short -> classType("Swift.Int16")
        }
    }

    private fun functionType(type: ObjCBlockPointerType): SwiftType {
        val returnType = type.returnType
        val parameterTypes = type.parameterTypes

        val params = parameterTypes.asSequence()
            .map { objcType -> convert(objcType) }
            .map { swiftType -> SwiftFunctionDomainTypeImpl.ParameterItemImpl(swiftType) }
            .toList()
        val domain = typeFactory.createDomainTypeOfItems(params)
        val image = convert(returnType)

        return typeFactory.createFunctionType(domain, image, false, SwiftTypeAttributesInfo.EMPTY)
    }

    private fun convertReference(className: String, typeKind: SwiftGlobalApiNotes.Kind): String {
        return globalApiNotes.get(className, typeKind)?.let { apiNote ->
            //todo [medvedev] when converting generic argument, only swiftName should be used
            apiNote.swiftBridge ?: apiNote.swiftName
        } ?: className //todo [medvedev] process
    }

    private fun optionalType(component: SwiftType) = typeFactory.createOptionalType(component, context)
    private fun selfType() = findClass(symbol)?.let { clazz -> typeFactory.createSelfType(clazz, false) } ?: SwiftType.UNKNOWN
    private fun classType(ref: String) = typeFactory.createClassType(ref, context)
}

fun createClassType(ref: String, context: SwiftSymbol): SwiftClassType =
    typeFactory.createClassType(ref, SwiftContext.of(context))

private fun findClass(context: SwiftSymbol): SwiftTypeSymbol? {
    var cur = context
    while (true) {
        if (cur is SwiftTypeSymbol) return cur
        cur = cur.context ?: return null
    }
}

//classes, protocols & extensions have custom swift names in attribute "swift_name("...")"
val Stub<*>.swiftName: String
    get() = when (this) {
        is ObjCClass -> attributes
        is ObjCMethod -> attributes
        is ObjCProperty -> propertyAttributes
        else -> null
    }?.extractSwiftName() ?: name

private fun List<String>.extractSwiftName(): String? =
    find { attr -> attr.startsWith("swift_name") }
        ?.let { attr -> attr.substring(12, attr.indexOfOrNull('(', 12) ?: attr.length - 2) }  //swift_name("...")

internal val publicSwiftAttributes: SwiftAttributesInfo =
    SwiftAttributesInfoImpl.create(EnumSet.of(SwiftDeclarationSpecifiers.PUBLIC))

internal val openSwiftAttributes: SwiftAttributesInfo =
    SwiftAttributesInfoImpl.create(EnumSet.of(SwiftDeclarationSpecifiers.OPEN))
