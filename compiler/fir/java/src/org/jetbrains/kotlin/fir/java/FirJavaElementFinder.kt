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
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.resolveSupertypesInTheAir
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.jvm.KotlinFinderMarker

class FirJavaElementFinder(
    private val session: FirSession,
    project: Project
) : PsiElementFinder(), KotlinFinderMarker {
    private val psiManager = PsiManager.getInstance(project)
    private val firProvider = session.firProvider

    override fun findPackage(qualifiedName: String): PsiPackage? {
        if (firProvider.getClassNamesInPackage(FqName(qualifiedName)).isEmpty()) return null
        return PsiPackageImpl(psiManager, qualifiedName)
    }

    override fun getClasses(psiPackage: PsiPackage, scope: GlobalSearchScope): Array<PsiClass> {
        return firProvider
            .getClassNamesInPackage(FqName(psiPackage.qualifiedName))
            .mapNotNull { findClass(psiPackage.qualifiedName + "." + it.identifier, scope) }
            .toTypedArray()
    }

    override fun findClasses(qualifiedName: String, scope: GlobalSearchScope): Array<PsiClass> {
        return findClass(qualifiedName, scope)?.let(::arrayOf) ?: emptyArray()
    }

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? {
        if (qualifiedName.endsWith(".")) return null
        val classId = ClassId.topLevel(FqName(qualifiedName))

        val firClass =
            firProvider.getFirClassifierByFqName(classId) as? FirRegularClass
                ?: return null

        val ktFile = firClass.psi?.containingFile as KtFile
        val fileStub = createJavaFileStub(classId.packageFqName, listOf(ktFile))

        return buildStub(firClass, fileStub).psi
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
            firClass.typeParameters.map { Pair(it.name.asString(), arrayOf(CommonClassNames.JAVA_LANG_OBJECT)) }
        )

        val superTypeRefs = when {
            firClass.resolvePhase > FirResolvePhase.SUPER_TYPES -> firClass.superTypeRefs
            else -> firClass.resolveSupertypesInTheAir(session)
        }

        stub.addSupertypesReferencesLists(firClass, superTypeRefs, session)

        for (nestedClass in firClass.declarations.filterIsInstance<FirRegularClass>()) {
            buildStub(nestedClass, stub)
        }

        return stub
    }

}

private fun FirRegularClass.packFlags(): Int {
    var flags = when (visibility) {
        Visibilities.PRIVATE -> ModifierFlags.PRIVATE_MASK
        Visibilities.PROTECTED -> ModifierFlags.PROTECTED_MASK
        Visibilities.PUBLIC -> ModifierFlags.PUBLIC_MASK
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

private fun createJavaFileStub(packageFqName: FqName, files: Collection<KtFile>): PsiJavaFileStub {
    val javaFileStub = PsiJavaFileStubImpl(packageFqName.asString(), /*compiled = */true)
    javaFileStub.psiFactory = ClsStubPsiFactory.INSTANCE

    val fakeFile = object : ClsFileImpl(files.first().viewProvider) {
        override fun getStub() = javaFileStub

        override fun getPackageName() = packageFqName.asString()

        override fun isPhysical() = false

        override fun getText(): String {
            return files.singleOrNull()?.text ?: super.getText()
        }
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
        is ConeDefinitelyNotNullType, is ConeIntersectionType, is ConeStubType ->
            error("Unexpected type: $this [${this::class}]")
        is ConeLookupTagBasedType -> lookupTag.name.asString()
    }
}

private fun ConeClassLikeType.mapToCanonicalString(session: FirSession): String {
    return when (this) {
        is ConeClassErrorType -> ERROR_TYPE_STUB
        else -> fullyExpandedType(session).mapToCanonicalNoExpansionString(session)
    }
}

private fun ConeClassLikeType.mapToCanonicalNoExpansionString(session: FirSession): String {
    if (lookupTag.classId == StandardClassIds.Array) {
        return when (val typeProjection = typeArguments[0]) {
            is ConeStarProjection -> CommonClassNames.JAVA_LANG_OBJECT
            is ConeTypedProjection -> {
                if (typeProjection.kind == ProjectionKind.IN)
                    CommonClassNames.JAVA_LANG_VOID
                else
                    (typeProjection.type as ConeClassLikeType).mapToCanonicalString(session)
            }
            else -> ERROR_TYPE_STUB
        } + "[]"
    }

    val context = ConeTypeCheckerContext(isErrorTypeEqualsToAnything = false, isStubTypeEqualsToAnything = true, session = session)

    with(context) {
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

private fun ConeKotlinTypeProjection.mapToCanonicalString(session: FirSession): String {
    return when (this) {
        is ConeStarProjection -> "?"
        is ConeTypedProjection -> {
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
