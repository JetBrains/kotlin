/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.SpecialJvmAnnotations
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.createConstantOrError
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.findKotlinClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue
import org.jetbrains.kotlin.utils.toMetadataVersion

internal class AnnotationsLoader(private val session: FirSession, private val kotlinClassFinder: KotlinClassFinder) {
    private fun loadAnnotation(
        annotationClassId: ClassId, result: MutableList<FirAnnotation>,
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        val lookupTag = annotationClassId.toLookupTag()

        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
            private val argumentMap = mutableMapOf<Name, FirExpression>()

            override fun visit(name: Name?, value: Any?) {
                if (name != null) {
                    argumentMap[name] = createConstant(value)
                }
            }

            private fun ClassLiteralValue.toFirClassReferenceExpression(): FirClassReferenceExpression {
                val resolvedClassTypeRef = classId.toLookupTag().toDefaultResolvedTypeRef()
                return buildClassReferenceExpression {
                    classTypeRef = resolvedClassTypeRef
                    typeRef = buildResolvedTypeRef {
                        type = StandardClassIds.KClass.constructClassLikeType(arrayOf(resolvedClassTypeRef.type), false)
                    }
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

                    typeRef = buildResolvedTypeRef {
                        type = ConeClassLikeTypeImpl(
                            this@toEnumEntryReferenceExpression.toLookupTag(),
                            emptyArray(),
                            isNullable = false
                        )
                    }
                }
            }

            override fun visitClassLiteral(name: Name?, value: ClassLiteralValue) {
                if (name == null) return
                argumentMap[name] = buildGetClassCall {
                    val argument = value.toFirClassReferenceExpression()
                    argumentList = buildUnaryArgumentList(argument)
                    typeRef = argument.typeRef
                }
            }

            override fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name) {
                if (name == null) return
                argumentMap[name] = enumClassId.toEnumEntryReferenceExpression(enumEntryName)
            }

            override fun visitArray(name: Name?): KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor? {
                if (name == null) return null
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
                                val argument = value.toFirClassReferenceExpression()
                                argumentList = buildUnaryArgumentList(argument)
                                typeRef = argument.typeRef
                            }
                        )
                    }

                    override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                        val list = mutableListOf<FirAnnotation>()
                        val visitor = loadAnnotation(classId, list)
                        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                            override fun visitEnd() {
                                visitor.visitEnd()
                                elements.add(list.single())
                            }
                        }
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

            override fun visitAnnotation(name: Name?, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                if (name == null) return null
                val list = mutableListOf<FirAnnotation>()
                val visitor = loadAnnotation(classId, list)
                return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                    override fun visitEnd() {
                        visitor.visitEnd()
                        argumentMap[name] = list.single()
                    }
                }
            }

            override fun visitEnd() {
                // Do not load the @java.lang.annotation.Repeatable annotation instance generated automatically by the compiler for
                // Kotlin-repeatable annotation classes. Otherwise the reference to the implicit nested "Container" class cannot be
                // resolved, since that class is only generated in the backend, and is not visible to the frontend.
                if (isRepeatableWithImplicitContainer(lookupTag, argumentMap)) return

                result += buildAnnotation {
                    annotationTypeRef = lookupTag.toDefaultResolvedTypeRef()
                    argumentMapping = buildAnnotationArgumentMapping {
                        mapping.putAll(argumentMap)
                    }
                }
            }

            private fun createConstant(value: Any?): FirExpression {
                return value.createConstantOrError(session)
            }
        }
    }

    private fun isRepeatableWithImplicitContainer(lookupTag: ConeClassLikeLookupTag, argumentMap: Map<Name, FirExpression>): Boolean {
        if (lookupTag.classId != SpecialJvmAnnotations.JAVA_LANG_ANNOTATION_REPEATABLE) return false

        val getClassCall = argumentMap[StandardClassIds.Annotations.ParameterNames.value] as? FirGetClassCall ?: return false
        val classReference = getClassCall.argument as? FirClassReferenceExpression ?: return false
        val containerType = classReference.classTypeRef.coneType as? ConeClassLikeType ?: return false
        val classId = containerType.lookupTag.classId
        if (classId.outerClassId == null ||
            classId.shortClassName.asString() != JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME
        ) return false

        val klass = kotlinClassFinder.findKotlinClass(classId, session.languageVersionSettings.languageVersion.toMetadataVersion())
        return klass != null && SpecialJvmAnnotations.isAnnotatedWithContainerMetaAnnotation(klass)
    }

    internal fun loadAnnotationIfNotSpecial(
        annotationClassId: ClassId, result: MutableList<FirAnnotation>,
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        if (annotationClassId in SpecialJvmAnnotations.SPECIAL_ANNOTATIONS) return null
        return loadAnnotation(annotationClassId, result)
    }

    private fun ConeClassLikeLookupTag.toDefaultResolvedTypeRef(): FirResolvedTypeRef =
        buildResolvedTypeRef {
            type = constructClassType(emptyArray(), isNullable = false)
        }
}
