
package org.jetbrains.kotlin.fir.resolve.optimization

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.originalIfFakeOverride
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.abbreviatedType
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.ClassKind

class FirReachabilityAnalyzer(
    private val session: FirSession
) : FirVisitorVoid() {

    companion object {
        /**
         * Identifies the initial set of declarations from which reachability analysis begins.
         *
         * The set includes non-private API members, inline definitions, const properties, and other
         * structurally essential elements.
         */
        fun collectRoots(file: FirFile): List<FirDeclaration> {
            val roots = ArrayList<FirDeclaration>()

            /**
             * Recursively traverses declarations to collect the initial set for the reachability analysis.
             *
             * @param isPrivateScope true if we are traversing members of a private class.
             *        In this case, even public members are not considered roots because they cannot be accessed from outside.
             * @param isTopLevel true if these declarations are at the file level.
             *        Used to preserve top-level private classes which are package-private in JVM to allow compatibility.
             */
            fun addRoots(declarations: List<FirDeclaration>, isPrivateScope: Boolean, isTopLevel: Boolean) {
                for (decl in declarations) {
                    if (isRoot(decl, isPrivateScope, isTopLevel)) {
                        roots.add(decl)
                    }
                    if (decl is FirRegularClass) {
                        val isPrivate = decl.status.visibility == Visibilities.Private
                        addRoots(decl.declarations, isPrivate || isPrivateScope, isTopLevel = false)
                    }
                }
            }

            addRoots(file.declarations, isPrivateScope = false, isTopLevel = true)

            return roots
        }

        /**
         * True if a specific declaration qualifies as a root for reachability analysis.
         *
         * @param isPrivateScope true if currently traversing members of a private class.
         * @param isTopLevel true if the declaration is at the file level.
         */
        private fun isRoot(decl: FirDeclaration, isPrivateScope: Boolean, isTopLevel: Boolean): Boolean {
            if (isPrivateScope) return false
            if (decl is FirEnumEntry) return true
            if (decl.annotations.isNotEmpty()) return true
            if (decl is FirConstructor && decl.isPrimary) return true
            if (isTopLevel && decl is FirRegularClass) return true

            if (decl is FirMemberDeclaration) {
                val status = decl.status
                val vis = status.visibility

                if (status.isConst) return true
                if (status.isInline) return true
                if (vis == Visibilities.Public ||
                    vis == Visibilities.Protected ||
                    vis == Visibilities.Internal
                ) return true
            }

            return false
        }

        /**
         * True if a declaration serves as an anchor for anonymous objects.
         */
        private fun shouldPreserveAnchor(declaration: FirDeclaration): Boolean {
            return when (declaration) {
                is FirProperty -> declaration.initializer is FirAnonymousObjectExpression
                is FirFunction -> bodyHasAnonymousObject(declaration.body)
                is FirAnonymousInitializer -> bodyHasAnonymousObject(declaration.body)
                else -> false
            }
        }

        /**
         * Recursively checks if a code block contains any anonymous object expressions.
         */
        private fun bodyHasAnonymousObject(body: FirBlock?): Boolean {
            if (body == null) return false
            return body.statements.any { stmt ->
                stmt is FirAnonymousObjectExpression ||
                        (stmt is FirReturnExpression && stmt.result is FirAnonymousObjectExpression)
            }
        }
    }

    private val reachable = HashSet<FirBasedSymbol<*>>()
    private val worklist = ArrayDeque<FirDeclaration>()
    private val processed = HashSet<FirDeclaration>()

    /**
     * Performs a reachability analysis on the given file to determine which symbols are used.
     *
     * This method starts from a minimal set of declarations (e.g., public API) and transitively
     * traverses them to mark all used symbols.
     *
     * @return A set of all reachable [FirBasedSymbol]s.
     */
    fun collectReachableSymbols(file: FirFile): Set<FirBasedSymbol<*>> {
        val roots = collectRoots(file)
        roots.forEach { mark(it) }

        while (worklist.isNotEmpty()) {
            val current = worklist.removeFirst()
            if (processed.add(current)) current.accept(this)
        }

        return reachable
    }

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitRegularClass(regularClass: FirRegularClass) {
        regularClass.annotations.forEach { it.accept(this) }
        regularClass.superTypeRefs.forEach { it.accept(this) }
        regularClass.typeParameters.forEach { it.accept(this) }
        regularClass.contextParameters.forEach { it.accept(this) }

        regularClass.declarations.forEach { decl ->
            if (decl is FirConstructor && decl.isPrimary){
                // It is required for every class to have a primary constructor.
                mark(decl.symbol)
            }
            if (shouldPreserveAnchor(decl)) {
                // The backend derives the name of anonymous objects from its declaring parent.
                mark(decl.symbol)
            }
        }

        if (regularClass.status.isInline || regularClass.status.isValue) {
            regularClass.declarations.forEach{
                if (it is FirConstructor && it.isPrimary) {
                    // Properties corresponding to primary constructor parameters define the underlying
                    // storage of the inline class.
                    val parameterNames = it.valueParameters.mapTo(HashSet()) { it.name }
                    regularClass.declarations.forEach { decl ->
                        if (decl is FirProperty && decl.name in parameterNames) {
                            mark(decl)
                        }
                    }
                }

                if (it is FirNamedFunction && it.name.asString() == "equals") {
                    // Inline classes require 'equals' for specialized lowering.
                    mark(it.symbol)
                }
            }
        }

        if (regularClass.classKind == ClassKind.ENUM_CLASS) {
            regularClass.declarations.forEach {
                // Enum entries are instantiated via constructors.
                if (it is FirConstructor) mark(it.symbol)
            }
        }
    }

    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        visitElement(resolvedNamedReference)
        val symbol = resolvedNamedReference.resolvedSymbol
        mark(symbol)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        visitElement(resolvedQualifier)
        mark(resolvedQualifier.symbol)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
        visitElement(callableReferenceAccess)
        val symbol = (callableReferenceAccess.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
        mark(symbol)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        visitElement(resolvedTypeRef)
        val type = resolvedTypeRef.coneType
        val symbol = type.toSymbol(session)
        mark(symbol)

        val abbreviatedType = type.abbreviatedType
        if (abbreviatedType is ConeClassLikeType) {
            val aliasSymbol = abbreviatedType.lookupTag.toSymbol(session)
            mark(aliasSymbol)
        }

        if (type is ConeClassLikeType) {
            val lookupSymbol = type.lookupTag.toSymbol(session)
            if (lookupSymbol is FirTypeAliasSymbol) {
                mark(lookupSymbol)
            }
        }
    }

    /**
     * Marks a symbol as reachable and ensures its necessary dependencies are also marked.
     *
     * High-level entry point that handles structural dependencies.
     */
    @OptIn(ClassIdBasedLocality::class)
    private fun mark(symbol: FirBasedSymbol<*>?) {
        val declaration = symbol?.fir ?: return

        if (declaration is FirCallableDeclaration) {
            // Ensures the base symbol of a fake/substitution override exists.
            // Prevents "Unbound private symbol" errors in IR.
            declaration.originalIfFakeOverride()?.let { mark(it.symbol) }
        }

        val containingClassId = (symbol as? FirCallableSymbol<*>)?.callableId?.classId
        if (containingClassId != null && !containingClassId.isLocal) {
            // The containing class must be also reachable, otherwise the member becomes orphaned.
            val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(containingClassId)
            if (classSymbol != null) {
                mark(classSymbol)
            }
        }

        mark(declaration)
    }

    /**
     * Adds a declaration to the reachable set and schedules it for visitation if it wasn't already processed.
     *
     * Low-level function that actually commits the declaration to the reachable set and worklist
     */
    private fun mark(declaration: FirDeclaration) {
        if (reachable.add(declaration.symbol)) {
            worklist.add(declaration)
        }
    }

}
