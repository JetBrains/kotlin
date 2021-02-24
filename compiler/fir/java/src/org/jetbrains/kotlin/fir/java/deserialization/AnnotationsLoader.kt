/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.SpecialJvmAnnotations
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.buildUnaryArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.createConstantOrError
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirReferencePlaceholderForResolvedAnnotations
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue

internal class AnnotationsLoader(private val session: FirSession) {
    private fun loadAnnotation(
        annotationClassId: ClassId, result: MutableList<FirAnnotationCall>,
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        val lookupTag = ConeClassLikeLookupTagImpl(annotationClassId)

        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
            private val argumentMap = mutableMapOf<Name, FirExpression>()

            override fun visit(name: Name?, value: Any?) {
                if (name != null) {
                    argumentMap[name] = createConstant(value)
                }
            }

            private fun ClassLiteralValue.toFirClassReferenceExpression(): FirClassReferenceExpression {
                val literalLookupTag = ConeClassLikeLookupTagImpl(classId)
                return buildClassReferenceExpression {
                    classTypeRef = literalLookupTag.toDefaultResolvedTypeRef()
                }
            }

            private fun ClassId.toEnumEntryReferenceExpression(name: Name): FirExpression {
                return buildFunctionCall {
                    val entryPropertySymbol =
                        session.symbolProvider.getClassDeclaredPropertySymbols(
                            this@toEnumEntryReferenceExpression, name,
                        ).firstOrNull()

                    calleeReference = when {
                        entryPropertySymbol != null -> {
                            buildResolvedNamedReference {
                                this.name = name
                                resolvedSymbol = entryPropertySymbol
                            }
                        }
                        else -> {
                            buildErrorNamedReference {
                                diagnostic = ConeSimpleDiagnostic(
                                    "Strange deserialized enum value: ${this@toEnumEntryReferenceExpression}.$name",
                                    DiagnosticKind.Java,
                                )
                            }
                        }
                    }
                }
            }

            override fun visitClassLiteral(name: Name, value: ClassLiteralValue) {
                argumentMap[name] = buildGetClassCall {
                    argumentList = buildUnaryArgumentList(value.toFirClassReferenceExpression())
                }
            }

            override fun visitEnum(name: Name, enumClassId: ClassId, enumEntryName: Name) {
                argumentMap[name] = enumClassId.toEnumEntryReferenceExpression(enumEntryName)
            }

            override fun visitArray(name: Name): KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
                return object : KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
                    private val elements = mutableListOf<FirExpression>()

                    override fun visit(value: Any?) {
                        elements.add(createConstant(value))
                    }

                    override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                        elements.add(enumClassId.toEnumEntryReferenceExpression(enumEntryName))
                    }

                    override fun visitClassLiteral(value: ClassLiteralValue) {
                        elements.add(
                            buildGetClassCall {
                                argumentList = buildUnaryArgumentList(value.toFirClassReferenceExpression())
                            }
                        )
                    }

                    override fun visitEnd() {
                        argumentMap[name] = buildArrayOfCall {
                            argumentList = buildArgumentList {
                                arguments += elements
                            }
                        }
                    }
                }
            }

            override fun visitAnnotation(name: Name, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                val list = mutableListOf<FirAnnotationCall>()
                val visitor = loadAnnotation(classId, list)
                return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                    override fun visitEnd() {
                        visitor.visitEnd()
                        argumentMap[name] = list.single()
                    }
                }
            }

            override fun visitEnd() {
                result += buildAnnotationCall {
                    annotationTypeRef = lookupTag.toDefaultResolvedTypeRef()
                    argumentList = buildArgumentList {
                        for ((name, expression) in argumentMap) {
                            arguments += buildNamedArgumentExpression {
                                this.expression = expression
                                this.name = name
                                isSpread = false
                            }
                        }
                    }
                    calleeReference = FirReferencePlaceholderForResolvedAnnotations
                }
            }

            private fun createConstant(value: Any?): FirExpression {
                return value.createConstantOrError(session)
            }
        }
    }

    internal fun loadAnnotationIfNotSpecial(
        annotationClassId: ClassId, result: MutableList<FirAnnotationCall>,
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        if (annotationClassId in SpecialJvmAnnotations.SPECIAL_ANNOTATIONS) return null
        return loadAnnotation(annotationClassId, result)
    }

    private fun ConeClassLikeLookupTag.toDefaultResolvedTypeRef(): FirResolvedTypeRef =
        buildResolvedTypeRef {
            type = constructClassType(emptyArray(), isNullable = false)
        }
}
