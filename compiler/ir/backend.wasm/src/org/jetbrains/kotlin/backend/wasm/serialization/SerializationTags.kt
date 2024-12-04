/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.serialization

internal object FunctionTags {
    const val DEFINED = 0u
    const val IMPORTED = 1u
}

internal object TypeDeclarationTags {
    const val FUNCTION = 0u
    const val STRUCT = 1u
    const val ARRAY = 2u
}

internal object TypeTags {
    const val REF = 0u
    const val REF_NULL = 1u
    const val ANYREF = 2u
    const val EQREF = 3u
    const val EXTERN_REF_TYPE = 4u
    const val EXTERN_REF = 5u
    const val F32 = 6u
    const val F64 = 7u
    const val FUNC_REF = 8u
    const val I16 = 9u
    const val I31_REF = 10u
    const val I32 = 11u
    const val I64 = 12u
    const val I8 = 13u
    const val NULL_EXTERN_REF_TYPE = 14u
    const val REF_NULL_EXTERN_REF_TYPE = 15u
    const val REF_NULL_REF_TYPE = 16u
    const val STRUCT_REF = 17u
    const val UNREACHABLE_TYPE = 18u
    const val V12 = 19u
}

internal object HeapTypeTags {
    const val ANY = 0u
    const val EQ = 1u
    const val EXTERN = 2u
    const val FUNC = 3u
    const val NO_EXTERN = 4u
    const val NONE = 5u
    const val STRUCT = 6u
    const val HEAP_TYPE = 7u
}

internal object InstructionTags {
    const val WITH_LOCATION = 0u
    const val WITHOUT_LOCATION = 1u
}

internal object ImmediateTags {
    const val BLOCK_TYPE_FUNCTION = 0u
    const val BLOCK_TYPE_VALUE = 1u
    const val CATCH = 2u
    const val CONST_F32 = 3u
    const val CONST_F64 = 4u
    const val CONST_I32 = 5u
    const val CONST_I64 = 6u
    const val CONST_STRING = 7u
    const val CONST_U8 = 8u
    const val DATA_INDEX = 9u
    const val ELEMENT_INDEX = 10u
    const val FUNC_INDEX = 11u
    const val GC_TYPE = 12u
    const val GLOBAL_INDEX = 13u
    const val HEAP_TYPE = 14u
    const val LABEL_INDEX = 15u
    const val LABEL_INDEX_VECTOR = 16u
    const val LOCAL_INDEX = 17u
    const val MEM_ARG = 18u
    const val MEMORY_INDEX = 19u
    const val STRUCT_FIELD_INDEX = 20u
    const val SYMBOL_I32 = 21u
    const val TABLE_INDEX = 22u
    const val TAG_INDEX = 23u
    const val TYPE_INDEX = 24u
    const val VALUE_TYPE_VECTOR = 25u
    const val BLOCK_TYPE_NULL_VALUE = 129u
}

internal object ImmediateCatchTags {
    const val CATCH = 0u
    const val CATCH_REF = 1u
    const val CATCH_ALL = 2u
    const val CATCH_ALL_REF = 3u
}

internal object TableValueTags {
    const val EXPRESSION = 0u
    const val FUNCTION = 1u
}

internal object ElementModeTags {
    const val ACTIVE = 0u
    const val DECLARATIVE = 1u
    const val PASSIVE = 2u
}

internal object ExportTags {
    const val FUNCTION = 0u
    const val TABLE = 1u
    const val MEMORY = 2u
    const val GLOBAL = 3u
    const val TAG = 4u
}

internal object LocationTags {
    const val NO_LOCATION = 0u
    const val LOCATION = 1u
    const val IGNORED_LOCATION = 2u
}

internal object IdSignatureTags {
    const val ACCESSOR = 0u
    const val COMMON = 1u
    const val COMPOSITE = 2u
    const val FILE_LOCAL = 3u
    const val LOCAL = 4u
    const val LOWERED_DECLARATION = 5u
    const val SCOPE_LOCAL_DECLARATION = 6u
    const val SPECIAL_FAKE_OVERRIDE = 7u
    const val FILE = 8u
}

internal object ConstantDataElementTags {
    const val CHAR_ARRAY = 0u
    const val CHAR_FIELD = 1u
    const val INT_ARRAY = 2u
    const val INT_FIELD = 3u
    const val INTEGER_ARRAY = 4u
    const val STRUCT = 5u
}

internal object NullableTags {
    const val NULL = 0u
    const val NOT_NULL = 1u
}