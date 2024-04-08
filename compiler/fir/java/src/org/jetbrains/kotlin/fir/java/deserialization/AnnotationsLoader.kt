/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.SpecialJvmAnnotations
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.createConstantOrError
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue
import org.jetbrains.kotlin.utils.toMetadataVersion

internal class AnnotationsLoader(private val session: FirSession, private val kotlinClassFinder: KotlinClassFinder) {
    private abstract inner class AnnotationsLoaderVisitorImpl : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        abstract fun visitExpression(name: Name?, expr: FirExpression)

        abstract val visitNullNames: Boolean

        override fun visit(name: Name?, value: Any?) {
            visitExpression(name, createConstant(value))
        }

        private fun ClassLiteralValue.toFirClassReferenceExpression(): FirClassReferenceExpression? {
            // toLookupTag will throw an exception if classId is local.
            // This should only happen in annotations of local declarations, in which we aren't interested anyway, so it should be fine
            // to just skip some of their arguments.
            if (classId.isLocal) return null

            val resolvedClassTypeRef = classId.toLookupTag().toDefaultResolvedTypeRef()
            return buildClassReferenceExpression {
                classTypeRef = resolvedClassTypeRef
                coneTypeOrNull = StandardClassIds.KClass.constructClassLikeType(arrayOf(resolvedClassTypeRef.type), false)
            }
        }

        override fun visitClassLiteral(name: Name?, value: ClassLiteralValue) {
            val argument = value.toFirClassReferenceExpression() ?: return

            visitExpression(name, buildGetClassCall {
                argumentList = buildUnaryArgumentList(argument)
                coneTypeOrNull = argument.resolvedType
            })
        }

        override fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name) {
            if (name == null && !visitNullNames) return
            visitExpression(name, createEnumEntryAccess(enumClassId, enumEntryName))
        }

        override fun visitArray(name: Name?): KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor? {
            if (name == null && !visitNullNames) return null
            return object : KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
                private val elements = mutableListOf<FirExpression>()

                override fun visit(value: Any?) {
                    elements.add(createConstant(value))
                }

                override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                    elements.add(createEnumEntryAccess(enumClassId, enumEntryName))
                }

                override fun visitClassLiteral(value: ClassLiteralValue) {
                    val argument = value.toFirClassReferenceExpression() ?: return
                    elements.add(buildGetClassCall {
                        argumentList = buildUnaryArgumentList(argument)
                        coneTypeOrNull = argument.resolvedType
                    })
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
                    visitExpression(name, buildArrayLiteral {
                        @OptIn(UnresolvedExpressionTypeAccess::class)
                        // For the array literal type, we use Array<Any> as an approximation; later FIR2IR will calculate more precise type
                        // See KT-62598
                        // FIR provides no guarantees on having exact type of deserialized array literals in annotations,
                        // including non-empty ones.
                        coneTypeOrNull = StandardClassIds.Any.constructClassLikeType().createOutArrayType()
                        argumentList = buildArgumentList {
                            arguments += elements
                        }
                    })
                }
            }
        }

        override fun visitAnnotation(name: Name?, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
            if (name == null && !visitNullNames) return null
            val list = mutableListOf<FirAnnotation>()
            val visitor = loadAnnotation(classId, list)
            return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                override fun visitEnd() {
                    visitor.visitEnd()
                    visitExpression(name, list.single())
                }
            }
        }

        private fun createConstant(value: Any?): FirExpression {
            return value.createConstantOrError(session)
        }
    }

    private fun loadAnnotation(
        annotationClassId: ClassId,
        result: MutableList<FirAnnotation>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        val lookupTag = annotationClassId.toLookupTag()

        return object : AnnotationsLoaderVisitorImpl() {
            private val argumentMap = mutableMapOf<Name, FirExpression>()

            override fun visitExpression(name: Name?, expr: FirExpression) {
                if (name != null) argumentMap[name] = expr
            }

            override val visitNullNames: Boolean = false

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
        }
    }

    internal fun loadAnnotationMethodDefaultValue(
        consumeResult: (FirExpression) -> Unit
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        return object : AnnotationsLoaderVisitorImpl() {
            var defaultValue: FirExpression? = null

            override fun visitExpression(name: Name?, expr: FirExpression) {
                defaultValue = expr
            }

            override val visitNullNames: Boolean = true

            override fun visitEnd() {
                defaultValue?.let(consumeResult)
            }
        }
    }

    private fun isRepeatableWithImplicitContainer(lookupTag: ConeClassLikeLookupTag, argumentMap: Map<Name, FirExpression>): Boolean {
        if (lookupTag.classId != SpecialJvmAnnotations.JAVA_LANG_ANNOTATION_REPEATABLE) return false

        val getClassCall = argumentMap[StandardClassIds.Annotations.ParameterNames.value] as? FirGetClassCall ?: return false
        val classReference = getClassCall.argument as? FirClassReferenceExpression ?: return false
        val containerType = classReference.classTypeRef.coneType as? ConeClassLikeType ?: return false
        val classId = containerType.lookupTag.classId
        if (classId.outerClassId == null || classId.shortClassName.asString() != JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME
        ) return false

        val klass = kotlinClassFinder.findKotlinClass(classId, session.languageVersionSettings.languageVersion.toMetadataVersion())
        return klass != null && SpecialJvmAnnotations.isAnnotatedWithContainerMetaAnnotation(klass)
    }

    internal fun loadAnnotationIfNotSpecial(
        annotationClassId: ClassId, result: MutableList<FirAnnotation>,
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        if (annotationClassId in SpecialJvmAnnotations.SPECIAL_ANNOTATIONS) return null
        // Note: we shouldn't resolve enum entries here either: KT-58294
        return loadAnnotation(annotationClassId, result)
    }

    private fun ConeClassLikeLookupTag.toDefaultResolvedTypeRef(): FirResolvedTypeRef =
        buildResolvedTypeRef {
            type = constructClassType(emptyArray(), isNullable = false)
        }

    private fun createEnumEntryAccess(classId: ClassId, name: Name): FirEnumEntryDeserializedAccessExpression =
        buildEnumEntryDeserializedAccessExpression {
            enumClassId = classId
            enumEntryName = name
        }
}
