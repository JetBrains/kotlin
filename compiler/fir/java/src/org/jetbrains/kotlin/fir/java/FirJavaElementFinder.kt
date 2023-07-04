/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.*
import com.intellij.psi.impl.cache.ModifierFlags
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.file.PsiPackageImpl
import com.intellij.psi.impl.java.stubs.*
import com.intellij.psi.impl.java.stubs.impl.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubElement
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.FirSupertypeResolverVisitor
import org.jetbrains.kotlin.fir.resolve.transformers.SupertypeComputationSession
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withConeTypeEntry
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.jvm.KotlinFinderMarker
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

class FirJavaElementFinder(
    private val session: FirSession,
    project: Project
) : PsiElementFinder(), KotlinFinderMarker {
    private val psiManager = PsiManager.getInstance(project)

    private val firProviders: List<FirProvider> = buildList {
        add(session.firProvider)
        session.collectAllDependentSourceSessions().mapTo(this) { it.firProvider }
    }

    override fun findPackage(qualifiedName: String): PsiPackage? {
        if (firProviders.none { it.symbolProvider.getPackage(FqName(qualifiedName)) != null }) return null
        return PsiPackageImpl(psiManager, qualifiedName)
    }

    override fun getClasses(psiPackage: PsiPackage, scope: GlobalSearchScope): Array<PsiClass> {
        return firProviders.flatMap { firProvider ->
            firProvider.getClassNamesInPackage(FqName(psiPackage.qualifiedName))
                .mapNotNull { findClass(psiPackage.qualifiedName + "." + it.identifier, scope) }
        }.toTypedArray()
    }

    override fun findClasses(qualifiedName: String, scope: GlobalSearchScope): Array<PsiClass> {
        return findClass(qualifiedName, scope)?.let { arrayOf(it) } ?: emptyArray()
    }

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? {
        if (qualifiedName.endsWith(".")) return null

        val fqName = FqName(qualifiedName)

        for (topLevelClass in generateSequence(fqName) { it.parentOrNull() }) {
            if (topLevelClass.isRoot) break
            val classId = ClassId.topLevel(topLevelClass)

            val firClass = firProviders.firstNotNullOfOrNull { it.getFirClassifierByFqName(classId) as? FirRegularClass } ?: continue

            val fileStub = createJavaFileStub(classId.packageFqName, psiManager)
            val topLevelResult = buildStub(firClass, fileStub).psi
            val tail = fqName.tail(topLevelClass).pathSegments()

            return tail.fold(topLevelResult) { psiClass, segment ->
                psiClass.findInnerClassByName(segment.identifier, false) ?: return null
            }
        }

        return null
    }

    private fun buildStub(firClass: FirRegularClass, parent: StubElement<*>): PsiClassStub<*> {
        val classId = firClass.classId
        val stub = PsiClassStubImpl<ClsClassImpl>(
            JavaStubElementTypes.CLASS, parent, classId.asSingleFqName().asString(), firClass.name.identifier, null,
            PsiClassStubImpl.packFlags(
                false,
                firClass.classKind == ClassKind.INTERFACE,
                firClass.classKind == ClassKind.ENUM_CLASS, false, false,
                firClass.classKind == ClassKind.ANNOTATION_CLASS, false, false,
                false, false, false
            )
        )

        PsiModifierListStubImpl(stub, firClass.packFlags())

        newTypeParameterList(
            stub,
            firClass.typeParameters.filterIsInstance<FirTypeParameter>().map { Pair(it.name.asString(), arrayOf(CommonClassNames.JAVA_LANG_OBJECT)) }
        )

        val superTypeRefs = when {
            firClass.superTypeRefs.all { it is FirResolvedTypeRef } -> firClass.superTypeRefs
            else -> firClass.resolveSupertypesOnAir(session)
        }

        stub.addSupertypesReferencesLists(firClass, superTypeRefs, session)

        for (nestedClass in firClass.declarations.filterIsInstance<FirRegularClass>()) {
            buildStub(nestedClass, stub)
        }

        return stub
    }

}

private fun FirRegularClass.resolveSupertypesOnAir(session: FirSession): List<FirTypeRef> {
    val visitor = FirSupertypeResolverVisitor(session, SupertypeComputationSession(), ScopeSession())
    return visitor.withFile(session.firProvider.getFirClassifierContainerFile(this.symbol)) {
        visitor.resolveSpecificClassLikeSupertypes(this, superTypeRefs)
    }
}

private fun FirSession.collectAllDependentSourceSessions(): List<FirSession> {
    val result = mutableListOf<FirSession>()
    collectAllDependentSourceSessionsTo(result)
    return result
}

private fun FirSession.collectAllDependentSourceSessionsTo(destination: MutableList<FirSession>) {
    val moduleData = moduleData
    collectAllDependentSourceSessionsTo(destination, moduleData.dependencies)
    collectAllDependentSourceSessionsTo(destination, moduleData.friendDependencies)
    collectAllDependentSourceSessionsTo(destination, moduleData.dependsOnDependencies)
}

private fun collectAllDependentSourceSessionsTo(destination: MutableList<FirSession>, dependencies: Collection<FirModuleData>) {
    for (dependency in dependencies) {
        val dependencySession = dependency.session
        if (dependencySession.kind != FirSession.Kind.Source) continue
        destination += dependencySession
        dependencySession.collectAllDependentSourceSessionsTo(destination)
    }
}

private fun FirRegularClass.packFlags(): Int {
    var flags = when (visibility) {
        Visibilities.Private -> ModifierFlags.PRIVATE_MASK
        Visibilities.Protected -> ModifierFlags.PROTECTED_MASK
        Visibilities.Public -> ModifierFlags.PUBLIC_MASK
        else -> ModifierFlags.PACKAGE_LOCAL_MASK
    }

    flags = flags or when (modality) {
        Modality.FINAL -> ModifierFlags.FINAL_MASK
        Modality.ABSTRACT -> ModifierFlags.ABSTRACT_MASK
        else -> 0
    }

    if (classId.isNestedClass && !isInner) {
        flags = flags or ModifierFlags.STATIC_MASK
    }

    return flags
}

private fun PsiClassStubImpl<*>.addSupertypesReferencesLists(
    firRegularClass: FirRegularClass,
    superTypeRefs: List<FirTypeRef>,
    session: FirSession
) {
    require(superTypeRefs.all { it is FirResolvedTypeRef }) {
        "Supertypes for light class $qualifiedName are being added too early"
    }

    val isInterface = firRegularClass.classKind == ClassKind.INTERFACE

    val interfaceNames = mutableListOf<String>()
    var superName: String? = null

    for (superTypeRef in superTypeRefs) {
        val superConeType = superTypeRef.coneTypeSafe<ConeClassLikeType>() ?: continue
        val supertypeFirClass = superConeType.toFirClass(session) ?: continue

        val canonicalString = superConeType.mapToCanonicalString(session)

        if (isInterface || supertypeFirClass.classKind == ClassKind.INTERFACE) {
            interfaceNames.add(canonicalString)
        } else {
            superName = canonicalString
        }
    }

    if (this.isInterface) {
        if (interfaceNames.isNotEmpty() && this.isAnnotationType) {
            interfaceNames.remove(CommonClassNames.JAVA_LANG_ANNOTATION_ANNOTATION)
        }
        newReferenceList(JavaStubElementTypes.EXTENDS_LIST, this, ArrayUtil.toStringArray(interfaceNames))
        newReferenceList(JavaStubElementTypes.IMPLEMENTS_LIST, this, ArrayUtil.EMPTY_STRING_ARRAY)
    } else {
        if (superName == null || "java/lang/Object" == superName || this.isEnum && "java/lang/Enum" == superName) {
            newReferenceList(JavaStubElementTypes.EXTENDS_LIST, this, ArrayUtil.EMPTY_STRING_ARRAY)
        } else {
            newReferenceList(JavaStubElementTypes.EXTENDS_LIST, this, arrayOf(superName))
        }
        newReferenceList(JavaStubElementTypes.IMPLEMENTS_LIST, this, ArrayUtil.toStringArray(interfaceNames))
    }

}

private fun newReferenceList(type: JavaClassReferenceListElementType, parent: StubElement<*>, types: Array<String>) {
    PsiClassReferenceListStubImpl(type, parent, types)
}

private fun newTypeParameterList(parent: StubElement<*>, parameters: List<Pair<String, Array<String>>>) {
    val listStub = PsiTypeParameterListStubImpl(parent)
    for (parameter in parameters) {
        val parameterStub = PsiTypeParameterStubImpl(listStub, parameter.first)
        newReferenceList(JavaStubElementTypes.EXTENDS_BOUND_LIST, parameterStub, parameter.second)
    }
}

private fun createJavaFileStub(packageFqName: FqName, psiManager: PsiManager): PsiJavaFileStub {
    val javaFileStub = PsiJavaFileStubImpl(packageFqName.asString(), /*compiled = */true)
    javaFileStub.psiFactory = ClsStubPsiFactory.INSTANCE

    val fakeFile = object : ClsFileImpl(DummyHolderViewProvider(psiManager)) {
        override fun getStub() = javaFileStub

        override fun getPackageName() = packageFqName.asString()

        override fun isPhysical() = false
    }

    javaFileStub.psi = fakeFile
    return javaFileStub
}

private fun ConeClassLikeType.toFirClass(session: FirSession): FirRegularClass? {
    val expandedType = this.fullyExpandedType(session)
    return (expandedType.lookupTag.toSymbol(session) as? FirClassSymbol)?.fir as? FirRegularClass
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////
// JVM TYPE MAPPING
// TODO: reuse other type mapping implementations when possible
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
private const val ERROR_TYPE_STUB = CommonClassNames.JAVA_LANG_OBJECT

private fun ConeKotlinType.mapToCanonicalString(session: FirSession): String {
    return when (this) {
        is ConeClassLikeType -> mapToCanonicalString(session)
        is ConeTypeVariableType, is ConeFlexibleType, is ConeCapturedType,
        is ConeDefinitelyNotNullType, is ConeIntersectionType, is ConeStubType, is ConeIntegerLiteralType ->
            errorWithAttachment("Unexpected type: ${this::class.java}") {
                withConeTypeEntry("type", this@mapToCanonicalString)
            }
        is ConeLookupTagBasedType -> lookupTag.name.asString()
    }
}

private fun ConeClassLikeType.mapToCanonicalString(session: FirSession): String {
    return when (this) {
        is ConeErrorType -> ERROR_TYPE_STUB
        else -> fullyExpandedType(session).mapToCanonicalNoExpansionString(session)
    }
}

private fun ConeClassLikeType.mapToCanonicalNoExpansionString(session: FirSession): String {
    if (lookupTag.classId == StandardClassIds.Array) {
        return when (val typeProjection = typeArguments[0]) {
            is ConeStarProjection -> CommonClassNames.JAVA_LANG_OBJECT
            is ConeKotlinTypeProjection -> {
                if (typeProjection.kind == ProjectionKind.IN)
                    CommonClassNames.JAVA_LANG_VOID
                else
                    (typeProjection.type as ConeClassLikeType).mapToCanonicalString(session)
            }
            else -> ERROR_TYPE_STUB
        } + "[]"
    }

    with(session.typeContext) {
        val typeConstructor = typeConstructor()
        typeConstructor.getPrimitiveType()?.let { return JvmPrimitiveType.get(it).wrapperFqName.asString() }
        typeConstructor.getPrimitiveArrayType()?.let { return JvmPrimitiveType.get(it).javaKeywordName + "[]" }
        val kotlinClassFqName = typeConstructor.getClassFqNameUnsafe() ?: return ERROR_TYPE_STUB
        val mapped = JavaToKotlinClassMap.mapKotlinToJava(kotlinClassFqName)?.asSingleFqName() ?: kotlinClassFqName

        return mapped.toString() +
                typeArguments.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = ", ", prefix = "<", postfix = ">") { it.mapToCanonicalString(session) }
                    .orEmpty()
    }

}

private fun ConeTypeProjection.mapToCanonicalString(session: FirSession): String {
    return when (this) {
        is ConeStarProjection -> "?"
        is ConeKotlinTypeProjection -> {
            val wildcard = when (kind) {
                ProjectionKind.STAR -> error("Should be handled in the case above")
                ProjectionKind.IN -> "? super "
                ProjectionKind.OUT -> "? extends "
                ProjectionKind.INVARIANT -> ""
            }

            wildcard + type.mapToCanonicalString(session)
        }
        else -> ERROR_TYPE_STUB
    }
}
