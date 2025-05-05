package org.jetbrains.kotlin.arguments.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import kotlin.Boolean

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
