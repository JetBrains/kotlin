/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.builder.buildLabel
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.processAllContainingCallCandidates
import org.jetbrains.kotlin.fir.resolve.initialTypeOfCandidate
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.firClassLike
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.util.OperatorNameConventions

class FirCollectionLiteralResolver(
    private val transformer: FirBodyResolveTransformer,
    private val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents
) {
    private val session: FirSession get() = components.session
    private inline val inferenceComponents: InferenceComponents get() = session.inferenceComponents

    private val buildersForCollectionLiteral: MutableMap<FirCollectionLiteral, MutableMap<ClassId, Candidate>> = mutableMapOf()

    private fun processLiteral(cl: FirCollectionLiteral, fixedArguments: List<FixedArgument>): FirExpression {
        when (fixedArguments.size) {
            1 -> {
                cl.replaceArgumentType(
                    (fixedArguments.single().fixedType as? ConeKotlinType)?.toFirResolvedTypeRef(cl.source)
                        ?: error("cant infer type of CL arguments")
                )
            }
            2 -> {
                cl.replaceKeyArgumentType(
                    (fixedArguments.first().fixedType as? ConeKotlinType)?.toFirResolvedTypeRef(cl.source)
                        ?: error("cant infer type of CL keys")
                )
                cl.replaceValueArgumentType(
                    (fixedArguments.last().fixedType as? ConeKotlinType)?.toFirResolvedTypeRef(cl.source)
                        ?: error("cant infer type of CL values")
                )
            }
            else -> {
                error("illegal state")
            }
        }


        val builders = components.callResolver.collectAvailableBuildersForCollectionLiteral(cl)

        if (builders.isEmpty()) {
            return buildErrorExpression(
                cl.source,
                ConeSimpleDiagnostic(
                    "Collection literal has no builders in the current scope",
                    DiagnosticKind.NoBuildersForCollectionLiteralFound
                )
            )
        }

        val possibleTypes = builders.map { builder ->
            // Перенести проверку в ResolutionStage
//            require(builder.freshVariables.size == 1)

            val initialType = components.initialTypeOfCandidate(builder)
//            val resultType = components.returnTypeCalculator.tryCalculateReturnType(builder.symbol as FirCallableSymbol<*>).type

//            val tv = builder.freshVariables.single()
//            builder.csBuilder.fixVariable(tv, fixedArgumentType.fixedType, ConeFixVariableConstraintPosition(tv))
//            builder.substitutor = builder.csBuilder.buildCurrentSubstitutor() as ConeSubstitutor
            builder.substitutor.substituteOrSelf(initialType).also {
                buildersForCollectionLiteral.getOrPut(cl) {
                    mutableMapOf()
                }[it.classId!!] = builder
            }
//            resultType.also {
//                buildersForCollectionLiteral.getOrPut(cl) {
//                    mutableMapOf()
//                }[it.classId!!] = builder
//            }
        }

        val type = ConeTypeIntersector.intersectTypes(session.inferenceComponents.ctx, possibleTypes)

        cl.resultType = cl.resultType.resolvedTypeFromPrototype(type)

        return cl
    }

    fun processSequenceLiteral(cl: FirCollectionLiteral): FirStatement {
        require(cl.kind == CollectionLiteralKind.SEQ_LITERAL)

        val fixedArgumentType = typeOfArgumentsSequence(cl)
        return processLiteral(cl, listOf(fixedArgumentType))
    }

    fun processDictionaryLiteral(cl: FirCollectionLiteral): FirStatement {
        require(cl.kind == CollectionLiteralKind.DICT_LITERAL)

        val fixedArgumentsType = typeOfArgumentsDictionary(cl)
        return processLiteral(cl, listOf(fixedArgumentsType.first, fixedArgumentsType.second))
    }

    fun replaceCollectionLiterals(call: FirFunctionCall): FirFunctionCall {
        val candidate = (call.calleeReference as? FirNamedReferenceWithCandidate)?.candidate ?: return call
        val argumentMapping = candidate.argumentMapping ?: error("cant get argument mapping for $candidate")
        if (!argumentMapping.keys.any { it is FirCollectionLiteral }) return call

        val replacer = object : FirTransformer<Unit>() {
            override fun <E : FirElement> transformElement(element: E, data: Unit): E {
                @Suppress("UNCHECKED_CAST")
                return (element.transformChildren(this, data) as E)
            }

            override fun transformCollectionLiteral(collectionLiteral: FirCollectionLiteral, data: Unit): FirStatement {
                val param = argumentMapping[collectionLiteral]!!
                val builder = chooseBuilder(candidate, collectionLiteral, param.returnTypeRef)
                    ?: return cantChooseBuilder(collectionLiteral).also {
                        argumentMapping[it as FirExpression] = param
                        argumentMapping.remove(collectionLiteral)
                    }

                return createFunctionCallForCollectionLiteral(builder, collectionLiteral).transformSingle(
                    transformer,
                    ResolutionMode.ContextDependent
                ).also {
                    argumentMapping[it] = param
                    argumentMapping.remove(collectionLiteral)
                }
            }
        }
        return call.transformSingle(replacer, Unit)
    }

    private fun cantFindBuilder(cl: FirCollectionLiteral, classId: ClassId): FirStatement {
        return buildErrorExpression(
            cl.source,
            ConeSimpleDiagnostic(
                "Collection literal has no builder for ${classId.shortClassName} in the current scope",
                DiagnosticKind.NoBuildersForCollectionLiteralFound
            )
        )
    }

    private fun cantChooseBuilder(cl: FirCollectionLiteral): FirStatement {
        return buildErrorExpression(
            cl.source,
            ConeSimpleDiagnostic(
                "Cant choose builder",
                DiagnosticKind.CantChooseBuilder
            )
        )
    }

    fun replaceCollectionLiteral(collectionLiteral: FirCollectionLiteral, expectedType: FirTypeRef?): FirStatement {
        expectedType ?: return collectionLiteral
        val builder: Candidate = when (expectedType) {
            is FirImplicitTypeRef -> {
                val listBuilder = buildersForCollectionLiteral[collectionLiteral]?.get(StandardClassIds.List)
                // TODO по идеи такого быть не может
                listBuilder ?: return cantFindBuilder(collectionLiteral, StandardClassIds.List)
            }
            is FirResolvedTypeRef -> {
                val coneType = expectedType.coneType
                if (coneType is ConeTypeParameterType) {
                    error("")
                } else {
                    val classId = coneType.classId!!
                    buildersForCollectionLiteral[collectionLiteral]?.get(classId)
                        ?: return cantFindBuilder(collectionLiteral, classId)
                }
            }
            is FirTypeRefWithNullability -> TODO()
        }
        return createFunctionCallForCollectionLiteral(builder, collectionLiteral).transformSingle(
            transformer,
            ResolutionMode.ContextDependent
        )
    }

    private fun chooseBuilder(candidate: Candidate, cl: FirCollectionLiteral, type: FirTypeRef): Candidate? {
        val substituted = candidate.substitutor.substituteOrSelf(type.coneType)
        val typeConstructor = substituted.typeConstructor(candidate.system)
        val notFixed = candidate.system.currentStorage().notFixedTypeVariables[typeConstructor]
        val clType = if (notFixed != null) {
            val resultType = inferenceComponents.resultTypeResolver.findResultType(
                candidate.system,
                notFixed,
                TypeVariableDirectionCalculator.ResolveDirection.TO_SUBTYPE
            )
            val it = resultType as? ConeIntersectionType ?: error("not it type")
            val clType = it.alternativeType
            if (clType !in it.intersectedTypes) {
                return null
            }
            clType?.let {
                candidate.system.fixVariable(
                    notFixed.typeVariable,
                    it,
                    ConeFixVariableConstraintPosition(notFixed.typeVariable)
                )
            }
            clType
        } else {
            substituted
        } ?: error("")
        return buildersForCollectionLiteral[cl]?.get(clType.classId!!)
    }

    fun typeOfArgumentsSequence(cl: FirCollectionLiteral): FixedArgument {
        require(cl.kind == CollectionLiteralKind.SEQ_LITERAL)

        val expressions = cl.expressions.map { (it as FirCollectionLiteralEntrySingle).expression }
        return fixTypeOfExpressions(expressions, "T")
    }

    fun typeOfArgumentsDictionary(cl: FirCollectionLiteral): Pair<FixedArgument, FixedArgument> {
        require(cl.kind == CollectionLiteralKind.DICT_LITERAL)

        val expressions = cl
            .expressions
            .associate { entry -> (entry as FirCollectionLiteralEntryPair).let { it.key to it.value } }

        val keysType = fixTypeOfExpressions(expressions.keys, "K")
        val valuesType = fixTypeOfExpressions(expressions.values, "V")
        return keysType to valuesType
    }

    private fun fixTypeOfExpressions(expressions: Collection<FirExpression>, typeVariableName: String): FixedArgument {
        val system = inferenceComponents.createConstraintSystem()
//        system.addOtherSystem(components.context.inferenceSession.currentConstraintSystem)
        system.addOtherSystem(ConstraintStorage.Empty)

        // Создать typeVariable
        val typeVariable = ConeTypeVariable(typeVariableName) // Возможно в будущем стоит заменить на какой-нибудь конкретный класс
        system.registerVariable(typeVariable)

        val upperType = typeVariable.defaultType
        for (expression in expressions) {
            val lowerType = expression.typeRef.coneTypeUnsafe<ConeKotlinType>()
            if (expression is FirFunctionCall) {
                expression.candidate()?.system?.asReadOnlyStorage()?.let {
                    system.addOtherSystem(it)
                }
            }
            system.addSubtypeConstraintIfCompatible(
                lowerType,
                upperType,
                SimpleConstraintSystemConstraintPosition
            )
        }

        // Зафиксировать (если возможно?) тип аргумента и вернуть его
        val resultType = inferenceComponents.resultTypeResolver.findResultType(
            system,
            system.notFixedTypeVariables[typeVariable.typeConstructor]!!,
            TypeVariableDirectionCalculator.ResolveDirection.TO_SUBTYPE
        )
        system.fixVariable(typeVariable, resultType, ConeFixVariableConstraintPosition(typeVariable))
        return FixedArgument(typeVariable, resultType)
    }

    private fun createFunctionCallForCollectionLiteral(
        builder: Candidate, // replace to fir function
        cl: FirCollectionLiteral
    ): FirFunctionCall {
        val adds = cl.expressions.map {
            buildFunctionCall {
                calleeReference = buildSimpleNamedReference { name = Name.identifier("add") }
                argumentList = when (cl.kind) {
                    CollectionLiteralKind.SEQ_LITERAL -> {
                        it as FirCollectionLiteralEntrySingle
                        val expression = it.expression
                        buildUnaryArgumentList(
                            if (expression is FirCollectionLiteral) {
                                replaceCollectionLiteral(expression, cl.argumentType) as FirFunctionCall
                            } else {
                                expression
                            }
                        )
                    }
                    CollectionLiteralKind.DICT_LITERAL -> (it as FirCollectionLiteralEntryPair).let { entry ->
                        buildBinaryArgumentList(entry.key, entry.value)
                    }
                }
            }
        }
        val lambda = buildLambdaArgumentExpression {
            expression = buildAnonymousFunctionExpression {
                anonymousFunction = buildAnonymousFunction {
                    origin = FirDeclarationOrigin.Synthetic
                    moduleData = session.moduleData
                    body = buildBlock {
                        statements.addAll(adds)
                    }
                    returnTypeRef = buildImplicitTypeRef()
                    receiverTypeRef = buildImplicitTypeRef()
                    symbol = FirAnonymousFunctionSymbol()
                    isLambda = true
                    label = buildLabel {
                        // TODO check for name of candidate
                        name = when (cl.kind) {
                            CollectionLiteralKind.SEQ_LITERAL -> OperatorNameConventions.BUILD_LIST_CL
                            CollectionLiteralKind.DICT_LITERAL -> OperatorNameConventions.BUILD_MAP_CL
                        }.identifier
                    }

                }
            }
        }
        val explicitReceiver = buildQualifiedAccessExpression {
            calleeReference = buildSimpleNamedReference {
                val fir = (builder.symbol as FirNamedFunctionSymbol).fir
                name = fir.receiverTypeRef?.let { it.firClassLike(session)?.classId?.relativeClassName?.parent()?.shortName() }
                    ?: fir.dispatchReceiverType?.classId?.relativeClassName?.parent()?.shortName() ?: error("")
//                name = receiverName
            }
        }
        return buildFunctionCall {
            this.explicitReceiver = explicitReceiver
            calleeReference = buildSimpleNamedReference {
                name = when (cl.kind) {
                    CollectionLiteralKind.SEQ_LITERAL -> OperatorNameConventions.BUILD_LIST_CL
                    CollectionLiteralKind.DICT_LITERAL -> OperatorNameConventions.BUILD_MAP_CL
                }
            }
            cl.argumentType?.let {
                typeArguments.add(buildTypeProjectionWithVariance {
                    typeRef = it
                    variance = Variance.INVARIANT
                })
            }
            argumentList = buildBinaryArgumentList(
                buildConstExpression(null, ConstantValueKind.Int, cl.expressions.size),
                lambda
            )
        }
    }


    /*private*/ data class FixedArgument(
        val typeVariable: ConeTypeVariable,
        val fixedType: KotlinTypeMarker
    )
}