/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.ecj

import org.eclipse.jdt.internal.compiler.ast.ASTNode
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.MutableJavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.buildJavaClass
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaDeclarationList
import org.jetbrains.kotlin.fir.java.enhancement.FirLazyJavaAnnotationList
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Represents a Java class after ECJ processing.
 *
 * This class provides functionality to iterate over Java class declarations (API-related ones)
 * and pass them to a lambda for further processing.
 */
class EcjJavaClass(
    val classId: ClassId,
    private val typeDeclaration: TypeDeclaration
) {
    companion object {
        /**
         * Determines if a declaration is API-related (public or otherwise affecting the API).
         */
        fun isApiRelated(declaration: ASTNode): Boolean {
            // Check if the declaration is public or protected
            val modifiers = when (declaration) {
                is TypeDeclaration -> declaration.modifiers
                is MethodDeclaration -> declaration.modifiers
                is FieldDeclaration -> declaration.modifiers
                else -> return false
            }
            return modifiers and (ClassFileConstants.AccPublic or ClassFileConstants.AccProtected) != 0
        }
    }
    /**
     * Iterates over Java class declarations (API-related ones) and passes them to the provided [processor].
     *
     * @param processor A lambda that processes each declaration.
     */
    fun <T> processApiDeclarations(processor: (ASTNode) -> T): List<T> {
        val results = mutableListOf<T>()

        // For interfaces, all methods and fields are considered API-related
        val isInterface = (typeDeclaration.modifiers and ClassFileConstants.AccInterface) != 0

        // Process methods
        typeDeclaration.methods?.forEach { method ->
            // Skip class initializer blocks (Clinit)
            if (method.javaClass.simpleName == "Clinit") {
                return@forEach
            }

            if (isInterface || isApiRelated(method)) {
                results.add(processor(method))
            }
        }

        // Process fields
        typeDeclaration.fields?.forEach { field ->
            if (isInterface || isApiRelated(field)) {
                results.add(processor(field))
            }
        }

        // Process member types (nested classes)
        typeDeclaration.memberTypes?.forEach { memberType ->
            if (isApiRelated(memberType)) {
                results.add(processor(memberType))
            }
        }

        return results
    }

    /**
     * Determines if a declaration is API-related (public or otherwise affecting the API).
     */
    private fun isApiRelated(declaration: ASTNode): Boolean {
        return EcjJavaClass.isApiRelated(declaration)
    }
}

/**
 * A lazy implementation of [FirJavaDeclarationList] for [EcjJavaClass].
 *
 * This class lazily calculates the declarations of a Java class using the [EcjJavaClass.processApiDeclarations] method.
 */
private class EcjLazyJavaDeclarationList(
    private val ecjJavaClass: EcjJavaClass,
    private val classSymbol: FirRegularClassSymbol
) : FirJavaDeclarationList {
    /**
     * [LazyThreadSafetyMode.PUBLICATION] is used here to avoid any potential problems with deadlocks
     * as we cannot control how Java resolution will access [declarations].
     */
    override val declarations: List<FirDeclaration> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val declarations = mutableListOf<FirDeclaration>()

        // Process API declarations using the processApiDeclarations method of EcjJavaClass
        ecjJavaClass.processApiDeclarations { declaration ->
            // In a real implementation, we would convert the ECJ declaration to a FIR declaration
            // and add it to the declarations list. For now, we just return the declaration as is.
            declaration
        }

        // Return an empty list for now
        emptyList()
    }
}

/**
 * Converts an [EcjJavaClass] to a [FirJavaClass].
 *
 * This function builds a [FirJavaClass] from the API declarations found in the [EcjJavaClass].
 * It follows the same pattern as [org.jetbrains.kotlin.fir.java.FirJavaFacade.convertJavaClassToFir].
 *
 * @param classSymbol The symbol for the class being converted.
 * @param parentClassSymbol The symbol for the parent class, if any.
 * @param moduleData The module data for the class.
 * @return A [FirJavaClass] representing the converted class.
 */
fun EcjJavaClass.convertJavaClassToFir(
    classSymbol: FirRegularClassSymbol,
    parentClassSymbol: FirRegularClassSymbol?,
    moduleData: FirModuleData
): FirJavaClass {
    val javaTypeParameterStack = MutableJavaTypeParameterStack()

    if (parentClassSymbol != null) {
        @OptIn(SymbolInternals::class)
        val parentStack = (parentClassSymbol.fir as FirJavaClass).classJavaTypeParameterStack
        javaTypeParameterStack.addStack(parentStack)
    }

    val firJavaClass = buildJavaClass {
        containingClassSymbol = parentClassSymbol
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        annotationList = FirLazyJavaAnnotationList(object : JavaClass {
            override val fqName: FqName? = classId.asSingleFqName()
            override val supertypes: Collection<JavaClassifierType> = emptyList()
            override val innerClassNames: Collection<Name> = emptyList()
            override fun findInnerClass(name: Name): JavaClass? = null
            override val outerClass: JavaClass? = null
            override val isInterface: Boolean = false
            override val isAnnotationType: Boolean = false
            override val isEnum: Boolean = false
            override val isRecord: Boolean = false
            override val isSealed: Boolean = false
            override val permittedTypes: Sequence<JavaClassifierType> = emptySequence()
            override val lightClassOriginKind: org.jetbrains.kotlin.load.java.structure.LightClassOriginKind? = null
            override val methods: Collection<org.jetbrains.kotlin.load.java.structure.JavaMethod> = emptyList()
            override val fields: Collection<org.jetbrains.kotlin.load.java.structure.JavaField> = emptyList()
            override val constructors: Collection<org.jetbrains.kotlin.load.java.structure.JavaConstructor> = emptyList()
            override val recordComponents: Collection<org.jetbrains.kotlin.load.java.structure.JavaRecordComponent> = emptyList()
            override fun hasDefaultConstructor(): Boolean = false
            override val name: Name = classId.shortClassName
            override val isFromSource: Boolean = true
            override val annotations: Collection<org.jetbrains.kotlin.load.java.structure.JavaAnnotation> = emptyList()
            override val isDeprecatedInJavaDoc: Boolean = false
            override fun findAnnotation(fqName: FqName): org.jetbrains.kotlin.load.java.structure.JavaAnnotation? = null
            override val isAbstract: Boolean = false
            override val isStatic: Boolean = false
            override val isFinal: Boolean = false
            override val visibility: Visibilities.Public = Visibilities.Public
            override val typeParameters: List<JavaTypeParameter> = emptyList()
        }, moduleData)
        this.source = null
        this.moduleData = moduleData
        symbol = classSymbol
        name = classId.shortClassName
        this.isFromSource = true
        this.visibility = Visibilities.Public
        this.classKind = ClassKind.CLASS
        this.modality = Modality.FINAL
        this.isTopLevel = classId.outerClassId == null
        this.isStatic = false
        this.javaPackage = null
        this.javaTypeParameterStack = javaTypeParameterStack
        existingNestedClassifierNames += emptyList<Name>()
        scopeProvider = JavaScopeProvider

        val selfEffectiveVisibility = Visibilities.Public.toEffectiveVisibility(parentClassSymbol?.toLookupTag(), forClass = true)
        val parentEffectiveVisibility = EffectiveVisibility.Public

        val effectiveVisibility = parentEffectiveVisibility.lowerBound(selfEffectiveVisibility, moduleData.session.typeContext)

        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.FINAL,
            effectiveVisibility
        ).apply {
            this.isInner = !isTopLevel && !this@buildJavaClass.isStatic
            isFun = classKind == ClassKind.INTERFACE
        }

        superTypeRefs.add(
            buildResolvedTypeRef {
                coneType = StandardClassIds.Any.constructClassLikeType(emptyArray(), isMarkedNullable = false)
            }
        )

        declarationList = EcjLazyJavaDeclarationList(this@convertJavaClassToFir, classSymbol)
    }

    return firJavaClass
}
