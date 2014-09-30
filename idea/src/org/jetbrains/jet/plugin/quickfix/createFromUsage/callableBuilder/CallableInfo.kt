package org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder

import java.util.Collections
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.JetType
import kotlin.properties.Delegates
import com.intellij.util.ArrayUtil
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester
import org.jetbrains.jet.plugin.refactoring.EmptyValidator
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.util.supertypes
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

/**
 * Represents a concrete type or a set of types yet to be inferred from an expression.
 */
abstract class TypeInfo(val variance: Variance) {
    object Empty: TypeInfo(Variance.INVARIANT) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> = Collections.emptyList()
    }

    class ByExpression(val expression: JetExpression, variance: Variance): TypeInfo(variance) {
        override val possibleNamesFromExpression: Array<String> by Delegates.lazy {
            JetNameSuggester.suggestNamesForExpression(expression, EmptyValidator)
        }

        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                expression.guessTypes(builder.currentFileContext).flatMap { it.getPossibleSupertypes(variance) }
    }

    class ByType(val theType: JetType, variance: Variance, val keepUnsubstituted: Boolean = false): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                theType.getPossibleSupertypes(variance)
    }

    class ByReceiverType(variance: Variance): TypeInfo(variance) {
        override fun getPossibleTypes(builder: CallableBuilder): List<JetType> =
                (builder.placement as CallablePlacement.WithReceiver).receiverTypeCandidate.theType.getPossibleSupertypes(variance)
    }

    open val possibleNamesFromExpression: Array<String> get() = ArrayUtil.EMPTY_STRING_ARRAY
    abstract fun getPossibleTypes(builder: CallableBuilder): List<JetType>

    protected fun JetType.getPossibleSupertypes(variance: Variance): List<JetType> {
        if (ErrorUtils.containsErrorType(this)) return Collections.singletonList(KotlinBuiltIns.getInstance().getAnyType())
        val single = Collections.singletonList(this)
        return when (variance) {
            Variance.IN_VARIANCE -> single + supertypes()
            else -> single
        }
    }
}

fun TypeInfo(expressionOfType: JetExpression, variance: Variance): TypeInfo = TypeInfo.ByExpression(expressionOfType, variance)
fun TypeInfo(theType: JetType, variance: Variance): TypeInfo = TypeInfo.ByType(theType, variance)

/**
 * Encapsulates information about a function parameter that is going to be created.
 */
class ParameterInfo(
        val typeInfo: TypeInfo,
        val preferredName: String? = null
)

enum class CallableKind {
    FUNCTION
    PROPERTY
}

class CallableInfo (
        val name: String,
        val kind: CallableKind,
        val receiverTypeInfo: TypeInfo,
        val returnTypeInfo: TypeInfo,
        val parameterInfos: List<ParameterInfo> = Collections.emptyList()
) {
    {
        if (kind == CallableKind.PROPERTY) assert (parameterInfos.isEmpty(), "$kind: Parameters are not allowed")
    }
}

fun createFunctionInfo(name: String,
                    receiverTypeInfo: TypeInfo,
                    returnTypeInfo: TypeInfo,
                    parameterInfos: List<ParameterInfo> = Collections.emptyList()): CallableInfo {
    return CallableInfo(name, CallableKind.FUNCTION, receiverTypeInfo, returnTypeInfo, parameterInfos)
}

fun createPropertyInfo(name: String,
                    receiverTypeInfo: TypeInfo,
                    returnTypeInfo: TypeInfo): CallableInfo {
    return CallableInfo(name, CallableKind.PROPERTY, receiverTypeInfo, returnTypeInfo)
}