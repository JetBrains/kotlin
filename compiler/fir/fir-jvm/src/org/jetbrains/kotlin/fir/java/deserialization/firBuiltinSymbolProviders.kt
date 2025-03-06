package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.impl.AbstractFirBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirFallbackBuiltinSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import java.io.InputStream

/**
 * This provider allows to get symbols for so-called built-in classes.
 *
 * Built-in classes are the classes which are considered mandatory for compiling any Kotlin code.
 * For this reason these classes must be provided even in a case when no standard library exists in classpath.
 * One can find a set of these classes inside libraries/stdlib/jvm/builtins directory.
 * In particular: all primitives, all arrays, collection-like interfaces, Any, Nothing, Unit, etc.
 *
 * It's used only for the JVM platform because some classes are mapped to Java classes and do not exist themselves,
 * so this provider is mandatory for the JVM compiler to work properly even with the standard library in classpath.
 * For non-JVM platforms, all / almost all built-in classes exist in the standard library, so this provider is not applicable.
 */
@NoMutableState
class FirJvmBuiltinsSymbolProvider(
    session: FirSession,
    private val fallbackBuiltinSymbolProvider: FirFallbackBuiltinSymbolProvider,
    findPackagePartData: (FqName) -> InputStream?,
) : FirSymbolProvider(session) {
    private val classpathBuiltinSymbolProvider: FirJvmClasspathBuiltinSymbolProvider = FirJvmClasspathBuiltinSymbolProvider(
        session,
        fallbackBuiltinSymbolProvider.moduleData,
        fallbackBuiltinSymbolProvider.kotlinScopeProvider,
        findPackagePartData,
    )

    override val symbolNamesProvider: FirSymbolNamesProvider
        get() = FirCompositeSymbolNamesProvider.fromSymbolProviders(listOf(classpathBuiltinSymbolProvider, fallbackBuiltinSymbolProvider))

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
        return classpathBuiltinSymbolProvider.getClassLikeSymbolByClassId(classId)
            ?: fallbackBuiltinSymbolProvider.getClassLikeSymbolByClassId(classId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val initSize = destination.size
        classpathBuiltinSymbolProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
        if (initSize == destination.size) {
            fallbackBuiltinSymbolProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        val initSize = destination.size
        classpathBuiltinSymbolProvider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        if (initSize == destination.size) {
            fallbackBuiltinSymbolProvider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        val initSize = destination.size
        classpathBuiltinSymbolProvider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        if (initSize == destination.size) {
            fallbackBuiltinSymbolProvider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        }
    }

    override fun hasPackage(fqName: FqName): Boolean {
        return classpathBuiltinSymbolProvider.hasPackage(fqName) || fallbackBuiltinSymbolProvider.hasPackage(fqName)
    }
}

/**
 * Uses classes defined in classpath to load builtins
 */
@ThreadSafeMutableState
class FirJvmClasspathBuiltinSymbolProvider(
    session: FirSession,
    moduleData: FirModuleData,
    kotlinScopeProvider: FirKotlinScopeProvider,
    val findPackagePartData: (FqName) -> InputStream?,
) : AbstractFirBuiltinSymbolProvider(session, moduleData, kotlinScopeProvider, false) {

    override val builtInsPackageFragments: Map<FqName, BuiltInsPackageFragment> = buildMap {
        StandardClassIds.builtInsPackages.forEach { fqName ->
            val inputStream = findPackagePartData(fqName) ?: return@forEach
            put(fqName, BuiltInsPackageFragment(inputStream))
        }
    }
}