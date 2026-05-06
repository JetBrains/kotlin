/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.components.containingDeclaration
import org.jetbrains.kotlin.analysis.api.components.dispatchReceiverType
import org.jetbrains.kotlin.analysis.api.components.render
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.impl.base.KaChainedSubstitutor
import org.jetbrains.kotlin.analysis.api.impl.base.KaMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.KaRendererKeywordFilter
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.useSiteSession
import org.jetbrains.kotlin.analysis.api.utils.getApiKClassOf
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.kdoc.psi.api.KDocCommentDescriptor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

@OptIn(KtNonPublicApi::class)
context(_: KaSession)
internal fun stringRepresentation(any: Any?): String = with(any) {
    fun KaType.render() = toString().replace('/', '.')
    return when (this) {
        null -> "null"
        is KaFunctionSymbol -> buildString {
            append(
                when (this@with) {
                    is KaNamedFunctionSymbol -> callableId ?: name
                    is KaSamConstructorSymbol -> callableId ?: name
                    is KaConstructorSymbol -> "<constructor>"
                    is KaPropertyGetterSymbol -> callableId ?: "<getter>"
                    is KaPropertySetterSymbol -> callableId ?: "<setter>"
                    is KaAnonymousFunctionSymbol -> "<anonymous function>"
                }
            )
            append("(")
            (this@with as? KaNamedFunctionSymbol)?.receiverParameter?.let { receiver ->
                append("<extension receiver>: ${receiver.returnType.render()}")
                if (valueParameters.isNotEmpty()) append(", ")
            }

            @Suppress("DEPRECATION")
            dispatchReceiverType?.let { dispatchReceiverType ->
                append("<dispatch receiver>: ${dispatchReceiverType.render()}")
                if (valueParameters.isNotEmpty()) append(", ")
            }

            valueParameters.joinTo(this) { stringRepresentation(it) }
            append(")")
            append(": ${returnType.render()}")
        }
        is KaValueParameterSymbol -> "${if (isVararg) "vararg " else ""}$name: ${returnType.render()}"
        // Receiver parameter should be rendered as it is because it is hard to cover it with tests overwise
        is KaReceiverParameterSymbol -> KaDebugRenderer().render(useSiteSession, this)
        is KaParameterSymbol -> "$name: ${returnType.render()}"
        is KaTypeParameterSymbol -> this.nameOrAnonymous.asString()
        is KaEnumEntrySymbol -> callableId?.toString() ?: name.asString()
        is KaVariableSymbol -> "${if (isVal) "val" else "var"} $name: ${returnType.render()}"
        is KaClassLikeSymbol -> classId?.toString() ?: nameOrAnonymous.asString()
        is KaPackageSymbol -> fqName.toString()
        is KaSymbol -> KaDebugRenderer().render(useSiteSession, this)
        is Boolean -> toString()
        is Map<*, *> -> if (isEmpty()) "{}" else entries.joinToString(
            separator = ",\n  ",
            prefix = "{\n  ",
            postfix = "\n}"
        ) { (k, v) -> "${k?.let { stringRepresentation(it).indented() }} -> (${v?.let { stringRepresentation(it).indented() }})" }
        is Collection<*> -> if (isEmpty()) "[]" else joinToString(
            separator = ",\n  ",
            prefix = "[\n  ",
            postfix = "\n]"
        ) {
            it?.let { stringRepresentation(it).indented() } ?: "null"
        }
        is PsiElement -> this.text
        is KaSubstitutor.Empty -> "<empty substitutor>"
        is KaMapBackedSubstitutor -> {
            val mappingText = getAsMap().entries
                .joinToString(prefix = "{", postfix = "}") { (k, v) -> stringRepresentation(k) + " = " + v }
            "<map substitutor: $mappingText>"
        }
        is KaChainedSubstitutor -> "${stringRepresentation(first)} then ${stringRepresentation(second)}"
        is KaSubstitutor -> "<complex substitutor>"
        is KaDiagnostic -> "$severity<$factoryName: $defaultMessage>"
        is KaType -> render()
        is Enum<*> -> name
        is Name -> asString()
        is CallableId -> toString()
        is KaCallableSignature<*> -> stringRepresentation(this)
        is KDocCommentDescriptor -> buildString {
            appendLine("<primary tag=\"${primaryTag.name}\" subject=\"${primaryTag.getSubjectName()}\">")
            append(primaryTag.getContent())
            additionalSections.forEach { section ->
                appendLine()
                appendLine("<section=\"${section.name}\" subject=\"${section.getSubjectName()}\">")
                append(section.getContent())
            }
        }
        else -> buildString {
            val className = renderFrontendIndependentKClassNameOf(this@with)
            append(className)

            val klass = this@with::class
            val multiCallResolutionAttemptCallValue: Any? = if (klass.isSubclassOf(KaMultiCallResolutionAttempt::class)) {
                klass.memberProperties
                    .firstOrNull { it.name == "call" }
                    ?.let {
                        @Suppress("UNCHECKED_CAST")
                        (it as KProperty1<Any, *>).get(this@with)
                    }
            } else {
                null
            }

            klass.memberProperties
                .filter { property ->
                    property.visibility == KVisibility.PUBLIC &&
                            !property.hasAnnotation<Deprecated>() &&
                            property.name != "token" &&
                            // The multi-call already renders all calls via other properties
                            !(klass.isSubclassOf(KaMultiCall::class) && property.name == KaMultiCall::calls.name) &&
                            /** The call is already covered as a part of [KaCompoundOperation.operationCall] */
                            !(klass.isSubclassOf(KaCompoundAccessCall::class) && property.name == KaCompoundAccessCall::operationCall.name) &&
                            /** This is already covered by [KaFunctionCall.valueArgumentMapping] and [KaFunctionCall.contextArguments] */
                            !(klass.isSubclassOf(KaFunctionCall::class) && property.name == KaFunctionCall<*>::combinedArgumentMapping.name) &&
                            // The multi-call resolution attempt already renders all attempts via individual named properties
                            !(klass.isSubclassOf(KaMultiCallResolutionAttempt::class) && property.name == KaMultiCallResolutionAttempt::attempts.name) &&
                            // If call is present, skip individual attempt properties (they're redundant with the call)
                            !(klass.isSubclassOf(KaMultiCallResolutionAttempt::class) && multiCallResolutionAttemptCallValue != null &&
                                    property.name.endsWith("Attempt")) &&
                            // If call is null, skip the call property itself
                            !(klass.isSubclassOf(KaMultiCallResolutionAttempt::class) && multiCallResolutionAttemptCallValue == null && property.name == "call")
                }.ifNotEmpty {
                    joinTo(this@buildString, separator = "\n  ", prefix = ":\n  ") { property ->
                        val name = property.name

                        @Suppress("UNCHECKED_CAST")
                        val value = (property as KProperty1<Any, *>).get(this@with)?.let { value ->
                            when {
                                (KaErrorCallInfo::class.isSuperclassOf(klass) || KaCallResolutionError::class.isSuperclassOf(klass)) && name == "candidateCalls" -> {
                                    sortedCalls(value as Collection<KaSingleOrMultiCall>)
                                }

                                KaSymbolResolutionError::class.isSuperclassOf(klass) && name == KaSymbolResolutionError::candidateSymbols.name -> {
                                    sortedSymbols(value as Collection<KaSymbol>)
                                }

                                else -> value
                            }
                        }

                        val valueAsString = value?.let { stringRepresentation(it).indented() }
                        "$name = $valueAsString"
                    }
                }
        }
    }
}

/**
 * Sequence of all declarations from the [KtFile] including nested declarations from classes.
 *
 * This API helps to iterate both compiled and source files without AST tree loading.
 */
val KtFile.allDeclarationsRecursively: Sequence<KtDeclaration>
    get() = if (isCompiled) {
        generateSequence<List<KtElement>>(listOf(this)) { elements ->
            elements.flatMap { element ->
                when (element) {
                    is KtFile -> element.declarations
                    is KtScript -> element.declarations
                    is KtClassOrObject -> element.declarations
                    else -> emptyList()
                }
            }.takeUnless(List<KtDeclaration>::isEmpty)
        }.flatten().filterIsInstance<KtDeclaration>()
    } else {
        collectDescendantsOfType<KtDeclaration>().asSequence()
    }

context(_: KaSession)
private fun stringRepresentation(signature: KaCallableSignature<*>): String = buildString {
    append(renderFrontendIndependentKClassNameOf(signature))

    val memberProperties = listOfNotNull(
        KaVariableSignature<*>::name.takeIf { signature is KaVariableSignature<*> },
        KaCallableSignature<*>::receiverType,
        KaCallableSignature<*>::returnType,
        KaCallableSignature<*>::symbol,
        KaFunctionSignature<*>::valueParameters.takeIf { signature is KaFunctionSignature<*> },
        KaCallableSignature<*>::contextParameters,
        KaCallableSignature<*>::callableId,
    )

    memberProperties.joinTo(this, separator = "\n  ", prefix = ":\n  ") { property ->
        @Suppress("UNCHECKED_CAST")
        val value = (property as KProperty1<Any, *>).get(signature)
        val valueAsString = value?.let { stringRepresentation(it).indented() }
        "${property.name} = $valueAsString"
    }
}

private fun String.indented() = replace("\n", "\n  ")

context(_: KaSession)
internal fun prettyPrintSignature(signature: KaCallableSignature<*>): String = prettyPrint {
    printCollectionIfNotEmpty(signature.contextParameters, prefix = "context(", postfix = ") ") { contextParameter ->
        append(contextParameter.name.asString())
        append(": ")
        append(contextParameter.returnType.render(position = Variance.INVARIANT))
    }

    when (signature) {
        is KaFunctionSignature -> {
            append("fun ")
            signature.receiverType?.let { append('.'); append(it.render(position = Variance.INVARIANT)) }
            append(signature.symbol.name?.asString())
            printCollection(signature.valueParameters, prefix = "(", postfix = ")") { parameter ->
                append(parameter.name.asString())
                append(": ")
                append(parameter.returnType.render(position = Variance.INVARIANT))
            }
            append(": ")
            append(signature.returnType.render(position = Variance.INVARIANT))
        }
        is KaVariableSignature -> {
            val symbol = signature.symbol
            append(if (symbol.isVal) "val" else "var")
            append(" ")
            signature.receiverType?.let { append('.'); append(it.render(position = Variance.INVARIANT)) }
            append(symbol.name.asString())
            append(": ")
            append(signature.returnType.render(position = Variance.INVARIANT))
        }
    }
}

context(_: KaSession)
internal fun sortedCalls(
    collection: Collection<KaSingleOrMultiCall>,
): Collection<KaSingleOrMultiCall> = collection.sortedWith { call1, call2 ->
    compareCalls(call1, call2)
}

context(_: KaSession)
internal fun sortedSymbols(
    collection: Collection<KaSymbol>,
): Collection<KaSymbol> = collection.sortedWith { symbol1, symbol2 ->
    compareSymbols(symbol1, symbol2)
}

context(_: KaSession)
internal fun compareSymbols(symbol1: KaSymbol, symbol2: KaSymbol): Int {
    return stringRepresentation(symbol1).compareTo(stringRepresentation(symbol2))
}

context(_: KaSession)
internal fun compareCalls(call1: KaSingleOrMultiCall, call2: KaSingleOrMultiCall): Int {
    return stringRepresentation(call1).compareTo(stringRepresentation(call2))
}

context(_: KaSession)
internal fun assertStableSymbolResult(
    testServices: TestServices,
    firstCandidates: List<KaCallCandidate>,
    secondCandidates: List<KaCallCandidate>,
) {
    val assertions = testServices.assertions
    assertions.assertEquals(firstCandidates.size, secondCandidates.size)

    for ((firstCandidate, secondCandidate) in firstCandidates.zip(secondCandidates)) {
        assertions.assertEquals(firstCandidate::class, secondCandidate::class)
        assertStableResult(testServices, firstCandidate.candidate, secondCandidate.candidate)
        assertions.assertEquals(firstCandidate.isInBestCandidates, secondCandidate.isInBestCandidates)

        when (firstCandidate) {
            is KaApplicableCallCandidate -> {}
            is KaInapplicableCallCandidate -> {
                assertStableResult(
                    testServices = testServices,
                    firstDiagnostic = firstCandidate.diagnostic,
                    secondDiagnostic = (secondCandidate as KaInapplicableCallCandidate).diagnostic,
                )
            }
        }
    }
}

context(_: KaSession)
internal fun assertStableResult(
    testServices: TestServices,
    firstAttempt: KaSymbolResolutionAttempt?,
    secondAttempt: KaSymbolResolutionAttempt?,
) {
    val assertions = testServices.assertions
    if (firstAttempt == null || secondAttempt == null) {
        assertions.assertEquals(firstAttempt, secondAttempt)
        return
    }

    assertions.assertEquals(firstAttempt::class, secondAttempt::class)
    if (firstAttempt is KaSymbolResolutionError) {
        assertStableResult(
            testServices = testServices,
            firstDiagnostic = firstAttempt.diagnostic,
            secondDiagnostic = (secondAttempt as KaSymbolResolutionError).diagnostic,
        )
    }

    if (firstAttempt is KaSymbolResolutionSuccess) {
        assertions.assertTrue(firstAttempt.symbols.isNotEmpty()) {
            "Success result has no symbols"
        }
    }

    if (firstAttempt is KaCompoundSymbolResolutionError) {
        assertMultiSymbolConsistency(testServices, firstAttempt)
    }

    val firstSymbols = sortedSymbols(firstAttempt.symbols)
    val secondSymbols = sortedSymbols(secondAttempt.symbols)
    assertions.assertEquals(firstSymbols.size, secondSymbols.size)

    for ((firstSymbol, secondSymbol) in firstSymbols.zip(secondSymbols)) {
        assertions.assertEquals(firstSymbol, secondSymbol)
    }
}

context(_: KaSession)
internal fun assertStableResult(
    mainElement: KtElement,
    testServices: TestServices,
    symbolResolutionAttempt: KaSymbolResolutionAttempt?,
    callResolutionAttempt: KaCallResolutionAttempt?,
) {
    val assertions = testServices.assertions

    when (callResolutionAttempt) {
        // Symbol resolution supports more cases than call resolution, so we check some guaranties only against it
        null -> return

        // Cannot check name reference expressions since they might have different result
        is KaCallResolutionError if mainElement is KtNameReferenceExpression -> {}

        is KaCallResolutionError -> {
            if (symbolResolutionAttempt !is KaSymbolResolutionError) {
                testServices.assertions.fail {
                    "${KaSymbolResolutionError::class.simpleName} is expected, but ${symbolResolutionAttempt?.let { it::class.simpleName }} is found"
                }
            }

            assertStableResult(
                testServices = testServices,
                firstDiagnostic = callResolutionAttempt.diagnostic,
                secondDiagnostic = symbolResolutionAttempt.diagnostic,
            )
        }

        is KaMultiCallResolutionAttempt -> if (symbolResolutionAttempt is KaCompoundSymbolResolutionError) {
            val callErrors = callResolutionAttempt.attempts.filterIsInstance<KaCallResolutionError>()
            val symbolErrors = symbolResolutionAttempt.attempts.filterIsInstance<KaSymbolResolutionError>()
            assertions.assertEquals(callErrors.size, symbolErrors.size) {
                "Number of error attempts differs between call and symbol resolution"
            }

            for ((callError, symbolError) in callErrors.zip(symbolErrors)) {
                assertStableResult(
                    testServices = testServices,
                    firstDiagnostic = callError.diagnostic,
                    secondDiagnostic = symbolError.diagnostic,
                )
            }
        }

        else -> {}
    }

    assertions.assertNotNull(symbolResolutionAttempt) {
        "Inconsistency: ${callResolutionAttempt::class.simpleName} found, but ${KaSymbolResolutionAttempt::class.simpleName} is null"
    }

    // Cannot check name reference expressions since they might have different result
    if (mainElement !is KtNameReferenceExpression) {
        val symbols = sortedSymbols(symbolResolutionAttempt!!.symbols)
        val symbolsFromCall = sortedSymbols(callResolutionAttempt.calls.flatMap(KaSingleOrMultiCall::symbols))
        assertions.assertEquals(expected = symbolsFromCall, actual = symbols)
    }
}

/**
 * The function returns a non-empty list of functions with the specified [functionName] and [receiverClass].
 */
internal fun KClass<KaResolver>.findSpecializedResolveFunctions(
    functionName: String,
    receiverClass: KClass<*>,
): List<KFunction<*>> = declaredFunctions.filter {
    it.name == functionName && it.extensionReceiverParameter?.type?.jvmErasure?.isSuperclassOf(receiverClass) == true
}.ifEmpty { error("No '$functionName' function found for ${receiverClass.simpleName}") }


context(_: KaSession)
internal fun assertStableResult(
    testServices: TestServices,
    firstAttempt: KaCallResolutionAttempt?,
    secondAttempt: KaCallResolutionAttempt?,
) {
    val assertions = testServices.assertions
    if (firstAttempt == null || secondAttempt == null) {
        assertions.assertEquals(firstAttempt, secondAttempt)
        return
    }

    assertions.assertEquals(firstAttempt::class, secondAttempt::class)

    when (firstAttempt) {
        is KaCallResolutionError -> {
            assertStableResult(
                testServices = testServices,
                firstDiagnostic = firstAttempt.diagnostic,
                secondDiagnostic = (secondAttempt as KaCallResolutionError).diagnostic,
            )
        }

        is KaCallResolutionSuccess -> {
            assertConsistency(testServices, firstAttempt.call)
        }

        is KaMultiCallResolutionAttempt -> {
            assertMultiCallConsistency(testServices, firstAttempt)

            val secondMulti = secondAttempt as KaMultiCallResolutionAttempt
            assertions.assertEquals(firstAttempt.attempts.size, secondMulti.attempts.size)
            for ((first, second) in firstAttempt.attempts.zip(secondMulti.attempts)) {
                assertions.assertEquals(first::class, second::class)
                if (first is KaCallResolutionError) {
                    assertStableResult(
                        testServices = testServices,
                        firstDiagnostic = first.diagnostic,
                        secondDiagnostic = (second as KaCallResolutionError).diagnostic,
                    )
                }
            }
        }
    }

    if (firstAttempt !is KaMultiCallResolutionAttempt) {
        val firstCalls = sortedCalls(firstAttempt.calls)
        val secondCalls = sortedCalls(secondAttempt.calls)
        assertions.assertEquals(firstCalls.size, secondCalls.size)

        for ((firstCall, secondCall) in firstCalls.zip(secondCalls)) {
            assertStableResult(testServices, firstCall, secondCall)
        }
    }
}

context(_: KaSession)
private fun assertMultiCallConsistency(testServices: TestServices, attempt: KaMultiCallResolutionAttempt) {
    val assertions = testServices.assertions
    val call = attempt.call
    if (call != null) {
        // All attempts must be successful
        for (subAttempt in attempt.attempts) {
            assertions.assertTrue(subAttempt is KaCallResolutionSuccess) {
                "Multi-call has non-null call, but attempt ${subAttempt::class.simpleName} is not success"
            }
        }
    } else {
        // At least one attempt must be an error
        assertions.assertTrue(attempt.attempts.any { it is KaCallResolutionError }) {
            "Multi-call has null call, but no error attempts found"
        }
    }
}

/**
 * The function forces [KaCompoundSymbolResolutionError] guarantees.
 */
context(_: KaSession)
private fun assertMultiSymbolConsistency(testServices: TestServices, attempt: KaCompoundSymbolResolutionError) {
    val assertions = testServices.assertions
    val attempts = attempt.attempts
    // At least one attempt must be an error
    assertions.assertTrue(attempts.any { it is KaSymbolResolutionError }) {
        "Multi-call has no error attempts found"
    }

    // At most one attempt must be successful
    assertions.assertTrue(attempts.count { it is KaSymbolResolutionSuccess } <= 1) {
        "Multi-call has more than one successful attempts found"
    }

    // At least two elements must be present
    assertions.assertTrue(attempts.size >= 2) {
        "Multi-call has less than two attempts found"
    }
}

context(_: KaSession)
internal fun assertStableResult(testServices: TestServices, firstCall: KaSingleOrMultiCall, secondCall: KaSingleOrMultiCall) {
    val assertions = testServices.assertions
    assertions.assertEquals(firstCall::class, secondCall::class)

    val symbolsFromFirstCall = sortedSymbols(firstCall.symbols)
    val symbolsFromSecondCall = sortedSymbols(secondCall.symbols)
    assertions.assertEquals(symbolsFromFirstCall.size, symbolsFromSecondCall.size)
    for ((first, second) in symbolsFromFirstCall.zip(symbolsFromSecondCall)) {
        assertions.assertEquals(first, second)
    }
}

context(_: KaSession)
internal fun assertConsistency(testServices: TestServices, call: KaSingleOrMultiCall, checkTypeArgumentsMapping: Boolean = true) {
    when (call) {
        is KaMultiCall -> {
            // Multi-call sub-calls may have empty type argument mappings
            val skipTypeArguments = true
            for (subCall in call.calls) {
                assertConsistency(testServices, subCall, checkTypeArgumentsMapping = !skipTypeArguments)
            }

            return
        }

        is KaSingleCall<*, *> -> {
            // The rest of the function body validates it
        }
    }

    val assertions = testServices.assertions
    if (call is KaCallableMemberCall<*, *>) {
        val partiallyAppliedSymbol = call.partiallyAppliedSymbol
        assertions.assertEquals(call.signature, partiallyAppliedSymbol.signature)
        assertions.assertEquals(call.dispatchReceiver, partiallyAppliedSymbol.dispatchReceiver)
        assertions.assertEquals(call.extensionReceiver, partiallyAppliedSymbol.extensionReceiver)
        assertions.assertEquals(call.contextArguments, partiallyAppliedSymbol.contextArguments)
    }

    @Suppress("DEPRECATION")
    if (call is KaSimpleFunctionCall) {
        assertions.assertEquals(call is KaImplicitInvokeCall, call.isImplicitInvoke)
    }

    if (call is KaFunctionCall<*>) {
        val combinedArgumentMapping = call.combinedArgumentMapping.toMutableMap()
        for ((expression, parameterFromSpecificMap) in call.valueArgumentMapping + call.contextArgumentMapping) {
            val parameterFromCombinedMap = combinedArgumentMapping.remove(expression)
            assertions.assertNotNull(parameterFromCombinedMap) {
                "Value argument for $parameterFromSpecificMap is not found in ${call::combinedArgumentMapping.name}: $combinedArgumentMapping"
            }

            assertions.assertEquals(parameterFromCombinedMap, parameterFromSpecificMap)
        }

        assertions.assertEquals(combinedArgumentMapping.size, 0) {
            "Extra elements found in ${call::combinedArgumentMapping.name}: $combinedArgumentMapping"
        }
    }

    if (checkTypeArgumentsMapping) {
        val typeArgumentsMapping = call.typeArgumentsMapping
        val typeParameters = call.symbol.typeParameters
        for (parameterSymbol in typeParameters) {
            val mappedType = typeArgumentsMapping[parameterSymbol]
            assertions.assertNotNull(mappedType) {
                "Type argument for type parameter $parameterSymbol is not found in $typeArgumentsMapping"
            }
        }

        assertions.assertEquals(typeParameters.size, typeArgumentsMapping.size) {
            "Extra elements found in ${call::typeArgumentsMapping.name}:\n${typeArgumentsMapping.keys - typeParameters}"
        }
    }
}

context(_: KaSession)
internal fun assertStableResult(
    testServices: TestServices,
    firstDiagnostic: KaDiagnostic,
    secondDiagnostic: KaDiagnostic,
) {
    val assertions = testServices.assertions
    assertions.assertEquals(firstDiagnostic.defaultMessage, secondDiagnostic.defaultMessage)
    assertions.assertEquals(firstDiagnostic.factoryName, secondDiagnostic.factoryName)
    assertions.assertEquals(firstDiagnostic.severity, secondDiagnostic.severity)
}

context(_: KaSession)
internal fun renderScopeWithParentDeclarations(scope: KaScope): String = prettyPrint {
    fun KaSymbol.qualifiedNameString() = when (this) {
        is KaConstructorSymbol -> "<constructor> ${containingClassId?.asString()}"
        is KaClassLikeSymbol -> classId!!.asString()
        is KaCallableSymbol -> callableId!!.toString()
        else -> error("unknown symbol $this")
    }

    val renderer = KaDeclarationRendererForSource.WITH_SHORT_NAMES.with {
        modifiersRenderer = modifiersRenderer.with {
            keywordsRenderer = keywordsRenderer.with { keywordFilter = KaRendererKeywordFilter.NONE }
        }
    }

    printCollection(scope.declarations.toList(), separator = "\n\n") { symbol ->
        val containingDeclaration = symbol.containingDeclaration as KaClassLikeSymbol
        append(symbol.render(renderer))
        append(" fromClass ")
        append(containingDeclaration.classId?.asString())
        if (symbol.typeParameters.isNotEmpty()) {
            appendLine()
            withIndent {
                printCollection(symbol.typeParameters, separator = "\n") { typeParameter ->
                    val containingDeclarationForTypeParameter = typeParameter.containingDeclaration
                    append(typeParameter.render(renderer))
                    append(" from ")
                    append(containingDeclarationForTypeParameter?.qualifiedNameString())
                }
            }
        }

        if (symbol is KaFunctionSymbol && symbol.valueParameters.isNotEmpty()) {
            appendLine()
            withIndent {
                printCollection(symbol.valueParameters, separator = "\n") { typeParameter ->
                    val containingDeclarationForValueParameter = typeParameter.containingDeclaration
                    append(typeParameter.render(renderer))
                    append(" from ")
                    append(containingDeclarationForValueParameter?.qualifiedNameString())
                }
            }
        }
    }
}

internal fun renderFrontendIndependentKClassNameOf(instanceOfClassToRender: Any): String {
    val classToRender = getApiKClassOf(instanceOfClassToRender)
    return classToRender.simpleName!!
}
