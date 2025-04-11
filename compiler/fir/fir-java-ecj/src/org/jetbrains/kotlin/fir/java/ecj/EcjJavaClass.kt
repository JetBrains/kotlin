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
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildField
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.MutableJavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.buildJavaClass
import org.jetbrains.kotlin.fir.java.declarations.buildJavaMethod
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaDeclarationList
import org.jetbrains.kotlin.fir.java.enhancement.FirLazyJavaAnnotationList
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.*

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
     * Converts this [EcjJavaClass] to a [FirJavaClass].
     *
     * This function builds a [FirJavaClass] from the API declarations found in this [EcjJavaClass].
     * It follows the same pattern as [org.jetbrains.kotlin.fir.java.FirJavaFacade.convertJavaClassToFir].
     *
     * @param classSymbol The symbol for the class being converted.
     * @param parentClassSymbol The symbol for the parent class, if any.
     * @param moduleData The module data for the class.
     * @return A [FirJavaClass] representing the converted class.
     */
    fun convertJavaClassToFir(
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

        // Determine class kind based on modifiers
        val isInterface = (typeDeclaration.modifiers and ClassFileConstants.AccInterface) != 0
        val isEnum = (typeDeclaration.modifiers and ClassFileConstants.AccEnum) != 0
        val isAnnotation = (typeDeclaration.modifiers and ClassFileConstants.AccAnnotation) != 0

        val classKind = when {
            isAnnotation -> ClassKind.ANNOTATION_CLASS
            isInterface -> ClassKind.INTERFACE
            isEnum -> ClassKind.ENUM_CLASS
            else -> ClassKind.CLASS
        }

        // Determine modality based on modifiers
        val isFinal = (typeDeclaration.modifiers and ClassFileConstants.AccFinal) != 0
        val isAbstract = (typeDeclaration.modifiers and ClassFileConstants.AccAbstract) != 0

        val modality = when {
            isInterface || isAnnotation -> Modality.ABSTRACT
            isAbstract -> Modality.ABSTRACT
            isFinal -> Modality.FINAL
            else -> Modality.OPEN
        }

        // Determine visibility based on modifiers
        val isPublic = (typeDeclaration.modifiers and ClassFileConstants.AccPublic) != 0
        val isProtected = (typeDeclaration.modifiers and ClassFileConstants.AccProtected) != 0
        val isPrivate = (typeDeclaration.modifiers and ClassFileConstants.AccPrivate) != 0

        val visibility = when {
            isPublic -> Visibilities.Public
            isProtected -> Visibilities.Protected
            isPrivate -> Visibilities.Private
            else -> JavaVisibilities.PackageVisibility
        }

        // Determine if the class is static
        val isStatic = (typeDeclaration.modifiers and ClassFileConstants.AccStatic) != 0

        // Get nested class names
        val nestedClassNames = typeDeclaration.memberTypes?.mapNotNull { 
            if (isApiRelated(it)) Name.identifier(String(it.name)) else null 
        } ?: emptyList()

        val firJavaClass = buildJavaClass {
            containingClassSymbol = parentClassSymbol
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            annotationList = FirLazyJavaAnnotationList(object : JavaClass {
                override val fqName: FqName? = classId.asSingleFqName()
                override val supertypes: Collection<JavaClassifierType> = emptyList()
                override val innerClassNames: Collection<Name> = nestedClassNames
                override fun findInnerClass(name: Name): JavaClass? = null
                override val outerClass: JavaClass? = null
                override val isInterface: Boolean = isInterface
                override val isAnnotationType: Boolean = isAnnotation
                override val isEnum: Boolean = isEnum
                override val isRecord: Boolean = false // ECJ doesn't have a specific flag for records
                override val isSealed: Boolean = false // ECJ doesn't have a specific flag for sealed classes
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
                override val isAbstract: Boolean = isAbstract
                override val isStatic: Boolean = isStatic
                override val isFinal: Boolean = isFinal
                override val visibility: Visibilities.Public = visibility as? Visibilities.Public ?: Visibilities.Public
                override val typeParameters: List<JavaTypeParameter> = emptyList()
            }, moduleData)
            this.source = null
            this.moduleData = moduleData
            symbol = classSymbol
            name = classId.shortClassName
            this.isFromSource = true
            this.visibility = visibility
            this.classKind = classKind
            this.modality = modality
            this.isTopLevel = classId.outerClassId == null
            this.isStatic = isStatic
            this.javaPackage = null
            this.javaTypeParameterStack = javaTypeParameterStack
            existingNestedClassifierNames += nestedClassNames
            scopeProvider = JavaScopeProvider

            val selfEffectiveVisibility = visibility.toEffectiveVisibility(parentClassSymbol?.toLookupTag(), forClass = true)
            val parentEffectiveVisibility = parentClassSymbol?.let {
                // We can't access originalStatus directly, so we'll use EffectiveVisibility.Public as a fallback
                EffectiveVisibility.Public
            } ?: EffectiveVisibility.Public

            val effectiveVisibility = parentEffectiveVisibility.lowerBound(selfEffectiveVisibility, moduleData.session.typeContext)

            status = FirResolvedDeclarationStatusImpl(
                visibility,
                modality,
                effectiveVisibility
            ).apply {
                this.isInner = !isTopLevel && !this@buildJavaClass.isStatic
                isFun = classKind == ClassKind.INTERFACE
            }

            // Add supertype references
            // For now, just add Any as the supertype
            superTypeRefs.add(
                buildResolvedTypeRef {
                    coneType = StandardClassIds.Any.constructClassLikeType(emptyArray(), isMarkedNullable = false)
                }
            )

            declarationList = EcjLazyJavaDeclarationList(this@EcjJavaClass, classSymbol)
        }

        return firJavaClass
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

        // Get the FirJavaClass and related data
        @OptIn(SymbolInternals::class)
        val firJavaClass = classSymbol.fir as FirJavaClass
        val moduleData = firJavaClass.moduleData
        val classId = classSymbol.classId
        val dispatchReceiver = firJavaClass.defaultType()

        // Process API declarations using the processApiDeclarations method of EcjJavaClass
        ecjJavaClass.processApiDeclarations { declaration ->
            when (declaration) {
                is MethodDeclaration -> {
                    // Convert method declaration to FIR
                    val methodName = Name.identifier(String(declaration.selector))
                    val methodId = CallableId(classId.packageFqName, classId.relativeClassName, methodName)
                    val methodSymbol = FirNamedFunctionSymbol(methodId)

                    val isStatic = (declaration.modifiers and ClassFileConstants.AccStatic) != 0
                    val isPublic = (declaration.modifiers and ClassFileConstants.AccPublic) != 0
                    val isProtected = (declaration.modifiers and ClassFileConstants.AccProtected) != 0
                    val isPrivate = (declaration.modifiers and ClassFileConstants.AccPrivate) != 0
                    val isFinal = (declaration.modifiers and ClassFileConstants.AccFinal) != 0
                    val isAbstract = (declaration.modifiers and ClassFileConstants.AccAbstract) != 0

                    val visibility = when {
                        isPublic -> Visibilities.Public
                        isProtected -> Visibilities.Protected
                        isPrivate -> Visibilities.Private
                        else -> JavaVisibilities.PackageVisibility
                    }

                    val modality = when {
                        isAbstract -> Modality.ABSTRACT
                        isFinal -> Modality.FINAL
                        else -> Modality.OPEN
                    }

                    val effectiveVisibility = visibility.toEffectiveVisibility(dispatchReceiver.lookupTag)

                    val firMethod = buildJavaMethod {
                        this.containingClassSymbol = classSymbol
                        this.moduleData = moduleData
                        this.isFromSource = true
                        this.name = methodName
                        this.symbol = methodSymbol
                        this.isStatic = isStatic

                        // For now, just use Unit as the return type
                        this.returnTypeRef = buildResolvedTypeRef {
                            coneType = StandardClassIds.Unit.constructClassLikeType(emptyArray(), isMarkedNullable = false)
                        }

                        this.status = FirResolvedDeclarationStatusImpl(
                            visibility,
                            modality,
                            effectiveVisibility
                        ).apply {
                            this.isStatic = isStatic
                            hasStableParameterNames = false
                        }

                        if (!isStatic) {
                            this.dispatchReceiverType = dispatchReceiver
                        }
                    }

                    declarations.add(firMethod)
                }
                is FieldDeclaration -> {
                    // Convert field declaration to FIR
                    val fieldName = Name.identifier(String(declaration.name))
                    val fieldId = CallableId(classId.packageFqName, classId.relativeClassName, fieldName)
                    val fieldSymbol = FirFieldSymbol(fieldId)

                    val isStatic = (declaration.modifiers and ClassFileConstants.AccStatic) != 0
                    val isPublic = (declaration.modifiers and ClassFileConstants.AccPublic) != 0
                    val isProtected = (declaration.modifiers and ClassFileConstants.AccProtected) != 0
                    val isPrivate = (declaration.modifiers and ClassFileConstants.AccPrivate) != 0
                    val isFinal = (declaration.modifiers and ClassFileConstants.AccFinal) != 0

                    val visibility = when {
                        isPublic -> Visibilities.Public
                        isProtected -> Visibilities.Protected
                        isPrivate -> Visibilities.Private
                        else -> JavaVisibilities.PackageVisibility
                    }

                    val modality = when {
                        isFinal -> Modality.FINAL
                        else -> Modality.OPEN
                    }

                    val effectiveVisibility = visibility.toEffectiveVisibility(dispatchReceiver.lookupTag)

                    val firField = buildField {
                        this.moduleData = moduleData
                        this.origin = FirDeclarationOrigin.Java.Source
                        this.name = fieldName
                        this.symbol = fieldSymbol
                        this.isVar = !isFinal

                        // For now, just use Int as the return type
                        this.returnTypeRef = buildResolvedTypeRef {
                            coneType = StandardClassIds.Int.constructClassLikeType(emptyArray(), isMarkedNullable = false)
                        }

                        this.status = FirResolvedDeclarationStatusImpl(
                            visibility,
                            modality,
                            effectiveVisibility
                        ).apply {
                            this.isStatic = isStatic
                        }

                        if (!isStatic) {
                            this.dispatchReceiverType = dispatchReceiver
                        }
                    }

                    declarations.add(firField)
                }
                // TODO: Add support for other declaration types (nested classes, etc.)
            }

            declaration
        }

        declarations
    }
}
