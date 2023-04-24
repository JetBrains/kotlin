/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.SpecialJvmAnnotations
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.toQualifiedPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.createConstantOrError
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredPropertySymbols
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.utils.toMetadataVersion

internal class AnnotationsLoader(private val session: FirSession, private val kotlinClassFinder: KotlinClassFinder) {
    private abstract inner class AnnotationsLoaderVisitorImpl(val enumEntryReferenceCreator: (ClassId, Name) -> FirExpression) : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        abstract fun visitExpression(name: Name?, expr: FirExpression)

        abstract val visitNullNames: Boolean

        abstract fun guessArrayTypeIfNeeded(name: Name?, arrayOfElements: List<FirExpression>): FirTypeRef?

        override fun visit(name: Name?, value: Any?) {
            visitExpression(name, createConstant(value))
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

        override fun visitClassLiteral(name: Name?, value: ClassLiteralValue) {
            visitExpression(name, buildGetClassCall {
                val argument = value.toFirClassReferenceExpression()
                argumentList = buildUnaryArgumentList(argument)
                typeRef = argument.typeRef
            })
        }

        override fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name) {
            if (name == null && !visitNullNames) return
            visitExpression(name, enumEntryReferenceCreator(enumClassId, enumEntryName))
        }

        override fun visitArray(name: Name?): KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor? {
            if (name == null && !visitNullNames) return null
            return object : KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
                private val elements = mutableListOf<FirExpression>()

                override fun visit(value: Any?) {
                    elements.add(createConstant(value))
                }

                override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                    elements.add(enumEntryReferenceCreator(enumClassId, enumEntryName))
                }

                override fun visitClassLiteral(value: ClassLiteralValue) {
                    elements.add(buildGetClassCall {
                        val argument = value.toFirClassReferenceExpression()
                        argumentList = buildUnaryArgumentList(argument)
                        typeRef = argument.typeRef
                    })
                }

                override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                    val list = mutableListOf<FirAnnotation>()
                    val visitor = loadAnnotation(classId, list, enumEntryReferenceCreator)
                    return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                        override fun visitEnd() {
                            visitor.visitEnd()
                            elements.add(list.single())
                        }
                    }
                }

                override fun visitEnd() {
                    visitExpression(name, buildArrayOfCall {
                        guessArrayTypeIfNeeded(name, elements)?.let { typeRef = it }
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
            val visitor = loadAnnotation(classId, list, enumEntryReferenceCreator)
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
        annotationClassId: ClassId, result: MutableList<FirAnnotation>, enumEntryReferenceCreator: (ClassId, Name) -> FirExpression
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        val lookupTag = annotationClassId.toLookupTag()

        return object : AnnotationsLoaderVisitorImpl(enumEntryReferenceCreator) {
            private val argumentMap = mutableMapOf<Name, FirExpression>()

            override fun visitExpression(name: Name?, expr: FirExpression) {
                if (name != null) argumentMap[name] = expr
            }

            override val visitNullNames: Boolean = false

            override fun guessArrayTypeIfNeeded(name: Name?, arrayOfElements: List<FirExpression>): FirTypeRef? {
                // Needed if we load a default value which is another annotation that has array value in it. e.g.:
                // To instantiate Deprecated() we need a default value for ReplaceWith() that has imports: Array<String> with default value [].
                if (name == null) return null
                // Note: generally we are not allowed to resolve anything, as this is might lead to recursive resolve problems
                // However, K1 deserializer did exactly the same and no issues were reported.
                val propS = session.symbolProvider.getClassDeclaredPropertySymbols(annotationClassId, name).firstOrNull()
                return propS?.resolvedReturnTypeRef
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
        }
    }

    internal fun loadAnnotationMethodDefaultValue(
        methodSignature: MemberSignature,
        consumeResult: (FirExpression) -> Unit
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        return object : AnnotationsLoaderVisitorImpl(this::toEnumEntryReferenceExpressionUnresolved) {
            var defaultValue: FirExpression? = null

            override fun visitExpression(name: Name?, expr: FirExpression) {
                defaultValue = expr
            }

            override val visitNullNames: Boolean = true

            override fun guessArrayTypeIfNeeded(name: Name?, arrayOfElements: List<FirExpression>): FirTypeRef {
                val descName = methodSignature.signature.substringAfterLast(')').removePrefix("[")
                val targetClassId = JvmPrimitiveType.getByDesc(descName)?.primitiveType?.typeFqName?.let { ClassId.topLevel(it) }
                    ?: FileBasedKotlinClass.resolveNameByInternalName(
                        descName.removePrefix("L").removeSuffix(";"),
                        // It seems that some inner classes info is required, but so far there are no problems with them (see six() in multimoduleCreation test)
                        FileBasedKotlinClass.InnerClassesInfo()
                    )
                return targetClassId.toLookupTag().constructClassType(arrayOf(), false).createOutArrayType().toFirResolvedTypeRef()
            }

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
        return loadAnnotation(annotationClassId, result, this::toEnumEntryReferenceExpressionWithResolve)
    }

    private fun ConeClassLikeLookupTag.toDefaultResolvedTypeRef(): FirResolvedTypeRef =
        buildResolvedTypeRef {
            type = constructClassType(emptyArray(), isNullable = false)
        }

    private fun toEnumEntryReferenceExpressionWithResolve(classId: ClassId, name: Name): FirPropertyAccessExpression =
        toEnumEntryReferenceExpressionUnresolved(classId, name).toQualifiedPropertyAccessExpression(session)

    private fun toEnumEntryReferenceExpressionUnresolved(classId: ClassId, name: Name): FirEnumEntryDeserializedAccessExpression =
        buildEnumEntryDeserializedAccessExpression {
            enumClassId = classId
            enumEntryName = name
        }
}
