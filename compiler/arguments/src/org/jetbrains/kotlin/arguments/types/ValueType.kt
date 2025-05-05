package org.jetbrains.kotlin.arguments.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import kotlin.Boolean

@Serializable
class IntType(
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false),
    override val defaultValue: ReleaseDependent<Int?> = ReleaseDependent(null),
) : KotlinArgumentValueType<Int> {
    override fun stringRepresentation(value: Int?): String {
        return "\"$value\""
    }
}

@Serializable
class StringArrayType(
    override val defaultValue: ReleaseDependent<Array<String>?> = ReleaseDependent(null),
) : KotlinArgumentValueType<Array<String>> {
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true)

    override fun stringRepresentation(value: Array<String>?): String {
        if (value == null) return "null"
        return value.joinToString(separator = ", ", prefix = "arrayOf(", postfix = ")") { "\"$it\""}
    }
}

@Serializable
class ExplicitApiModeType(
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false),
    override val defaultValue: ReleaseDependent<ExplicitApiMode?> = ReleaseDependent(ExplicitApiMode.DISABLED),
) : KotlinArgumentValueType<ExplicitApiMode> {
    override fun stringRepresentation(value: ExplicitApiMode?): String {
        return value?.state.valueOrNullStringLiteral
    }
}

@Serializable
class ReturnValueCheckerModeType(
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false),
    override val defaultValue: ReleaseDependent<ReturnValueCheckerMode?> = ReleaseDependent(ReturnValueCheckerMode.DISABLED),
) : KotlinArgumentValueType<ReturnValueCheckerMode> {
    override fun stringRepresentation(value: ReturnValueCheckerMode?): String {
        return value?.state.valueOrNullStringLiteral
    }
}

private val String?.valueOrNullStringLiteral: String
    get() = "\"${this}\""
