/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinAnnotationsResolver
import org.jetbrains.kotlin.analysis.api.platform.declarations.createAnnotationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLPartialBodyElementMapper
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.bodyBlock
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirIdeRegisteredPluginAnnotations
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.FirPredicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationType
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.registeredPluginAnnotations
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.findIsInstanceAnd
import org.junit.jupiter.api.Test
import kotlin.test.*

class LLGenericAnalysisTest : AbstractAnalysisApiExecutionTest("testData/genericAnalysis") {
    override val configurator = LLSourceLikeTestConfigurator()

    @Test
    fun simple(ktFile: KtFile) {
        withResolutionFacade(ktFile) { resolutionFacade ->
            val ktSecondCall = ktFile.descendantsOfType<KtCallExpression>().first { it.text == "consume(2)" }
            ktSecondCall.getOrBuildFir(resolutionFacade)

            val firFile = ktFile.getOrBuildFirFile(resolutionFacade)
            val firFunction = firFile.declarations.findIsInstanceAnd<FirFunction> { it.name() == "test" }!!

            // The function is only partially resolved
            assert(firFunction.resolvePhase == FirResolvePhase.ANNOTATION_ARGUMENTS)
        }
    }

    @Test
    fun bodyAlreadyAnalyzed(ktFile: KtFile) {
        withResolutionFacade(ktFile) { resolutionFacade ->
            val firFile = ktFile.getOrBuildFirFile(resolutionFacade)
            val firFunction = firFile.declarations.findIsInstanceAnd<FirFunction> { it.name() == "test" }!!
            assert(firFunction.resolvePhase == FirResolvePhase.RAW_FIR)

            // Do not trigger 'getOrBuildFir()'
            firFunction.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            assert(firFunction.resolvePhase == FirResolvePhase.BODY_RESOLVE)

            val ktFunction = ktFile.declarations.findIsInstanceAnd<KtFunction> { it.name == "test" }!!
            val ktBlock = ktFunction.bodyBlockExpression!!
            val ktStatements = ktBlock.statements

            // Simulate data race between BODY_RESOLVE and LLElementMapper computation
            val mapper = LLPartialBodyElementMapper(firFunction, ktFunction, ktBlock, ktStatements, firFunction.llFirResolvableSession!!)
            assertEquals(firFunction, mapper.invoke(ktFunction))

            val ktSecondCall = ktFile.descendantsOfType<KtCallExpression>().first { it.text == "consume(2)" }
            val firSecondCall = ktSecondCall.getOrBuildFirOfType<FirFunctionCall>(resolutionFacade)
            assertEquals(ktSecondCall, firSecondCall.psi)
        }
    }

    @Test
    fun abstractFunctionWithDefault(ktFile: KtFile, testServices: TestServices) {
        withResolutionFacade(ktFile) { resolutionFacade ->
            val targetPsiClass = ktFile.declarations.last() as KtClass
            val targetPsiFunction = targetPsiClass.declarations.last() as KtNamedFunction
            val targetFirFunction = targetPsiFunction.resolveToFirSymbolOfType<FirNamedFunctionSymbol>(resolutionFacade)
            val psiStatements = targetPsiFunction.bodyBlock?.statements.orEmpty()
            psiStatements.first().getOrBuildFirOfType<FirStatement>(resolutionFacade)

            fun renderTarget(element: FirElement) = lazyResolveRenderer(StringBuilder()).renderElementAsString(element).trim()

            testServices.assertions.assertEquals(
                """
                    public final [ResolvedTo(ANNOTATION_ARGUMENTS)] [PartialBodyAnalysisStateKey=1(1/2) #1] fun g(): R|kotlin/collections/List<kotlin/Int>| {
                        [ResolvedTo(BODY_RESOLVE)] lval e: R|kotlin/collections/List<T>| = Null(null)!!
                        ^g e#.myMap#(::f#)
                    }
                """.trimIndent(),
                renderTarget(targetFirFunction.fir),
            )

            val lastFirElement = executeOnPooledThreadInReadAction {
                psiStatements.last().getOrBuildFirOfType<FirElement>(resolutionFacade)
            }

            testServices.assertions.assertEquals(
                """
                        ^g R|<local>/e|.R|/myMap|<R|T|, R|kotlin/Int|>(::R|SubstitutionOverride</G.f: R|kotlin/Int|>|)
                    """.trimIndent(),
                renderTarget(lastFirElement),
            )
        }
    }

    /**
     * A regression test for KT-88945.
     *
     * [KotlinAnnotationsResolver] is the only annotation source for [FirPredicateBasedProvider] as long as the FIR annotations
     * of a declaration are not resolved yet, so a gap between the two makes plugin-generated declarations
     * depend on the amount of the performed resolution.
     */
    @Test
    fun redeclaredPluginAnnotation(ktFile: KtFile, testServices: TestServices) {
        assertAnnotationsOnMyClass(ktFile, testServices, "org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo")
    }

    /**
     * [KotlinAnnotationsResolver] doesn't expand type aliases: the compiler doesn't match `annotated` predicates on non-local
     * declarations through a type alias (KT-75685).
     */
    @Test
    fun annotationTypeAlias(ktFile: KtFile, testServices: TestServices) {
        assertAnnotationsOnMyClass(ktFile, testServices)
    }

    private fun assertAnnotationsOnMyClass(ktFile: KtFile, testServices: TestServices, vararg expected: String) {
        val project = ktFile.project
        val ktClass = ktFile.declarations.single { it.name == "MyClass" } as KtClass
        val annotationsResolver = project.createAnnotationResolver(GlobalSearchScope.allScope(project))

        testServices.assertions.assertEquals(
            expected = expected.mapTo(mutableSetOf()) { ClassId.topLevel(FqName(it)) },
            actual = annotationsResolver.annotationsOnDeclaration(ktClass),
        )
    }

    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun predicateTargets(ktFile: KtFile) {
        withResolutionFacade(ktFile) { resolutionFacade ->
            val provider = resolutionFacade.useSiteFirSession.predicateBasedProvider
            val annotation = FqName("org.jetbrains.kotlin.plugin.sandbox.TestTopLevelPrivateSuspendFun")

            fun getSymbolsByPredicate(predicate: LookupPredicate): Set<String> {
                return provider.getSymbolsByPredicate(predicate).mapTo(mutableSetOf()) { it.predicateTestName() }
            }

            fun lookup(type: DeclarationType): Set<String> {
                return getSymbolsByPredicate(LookupPredicate.create { annotated(annotation).matching(type) })
            }

            assertEquals(setOf("topLevelFunction"), lookup(DeclarationType.TOP_LEVEL_FUNCTION))
            assertEquals(setOf("Outer.memberFunction"), lookup(DeclarationType.MEMBER_FUNCTION))
            assertEquals(setOf("topLevelFunction", "Outer.memberFunction"), lookup(DeclarationType.FUNCTION))
            assertEquals(
                setOf("TopClass", "TopInterface", "TopObject", "TopEnum", "TopAnnotation", "Outer", "Outer.Nested"),
                lookup(DeclarationType.CLASS),
            )
            assertEquals(setOf("TopClass", "Outer", "Outer.Nested"), lookup(DeclarationType.CLASS_ONLY))
            assertEquals(
                setOf("TopInterface", "TopObject"),
                getSymbolsByPredicate(LookupPredicate.create {
                    annotated(annotation).matching(DeclarationType.INTERFACE) or
                            annotated(annotation).matching(DeclarationType.OBJECT)
                })
            )
            assertEquals(setOf("TopEnum.ENTRY"), lookup(DeclarationType.ENUM_ENTRY))
            assertEquals(setOf("Alias"), lookup(DeclarationType.TYPEALIAS))
            assertEquals(setOf("ConstructorOnly.ConstructorOnly"), lookup(DeclarationType.CONSTRUCTOR))
            assertEquals(setOf("topLevelProperty"), lookup(DeclarationType.TOP_LEVEL_PROPERTY))
            // Enum entries are property targets too, like with `@Target`. Getter-only annotations don't make the property match.
            assertEquals(setOf("topLevelProperty", "Outer.memberProperty", "TopEnum.ENTRY"), lookup(DeclarationType.PROPERTY))

            val topLevelFunctions = DeclarationPredicate.create {
                annotated(annotation).matching(DeclarationType.TOP_LEVEL_FUNCTION)
            }
            val functions = provider.getSymbolsByPredicate(
                LookupPredicate.create { annotated(annotation).matching(DeclarationType.FUNCTION) }
            )
            assertEquals(
                setOf("topLevelFunction"),
                functions.filter { provider.matches(topLevelFunctions, it) }.mapTo(mutableSetOf()) { it.predicateTestName() },
            )
            assertEquals(
                setOf("TopClass", "Outer"),
                lookup(DeclarationType.Custom {
                    // Only match top-level classes.
                    (it is FirRegularClass && it.classKind == ClassKind.CLASS && it.symbol.classId.outerClassId == null)
                }),
            )
        }
    }

    @Test
    fun matchingTypePredicates(ktFile: KtFile) {
        withResolutionFacade(ktFile) { resolutionFacade ->
            val provider = resolutionFacade.useSiteFirSession.predicateBasedProvider
            val annotation = FqName("org.jetbrains.kotlin.plugin.sandbox.TestTopLevelPrivateSuspendFun")
            val all = LookupPredicate.create { annotated(annotation) }
            val functions = LookupPredicate.create { annotated(annotation).matching(DeclarationType.FUNCTION) }
            val classes = LookupPredicate.create { annotated(annotation).matching(DeclarationType.CLASS_ONLY) }

            fun lookup(predicate: LookupPredicate): Set<String> =
                provider.getSymbolsByPredicate(predicate).mapTo(mutableSetOf()) { it.predicateTestName() }

            assertEquals(setOf("topLevelFunction", "Parent.annotatedMember", "Parent.Companion.companionFunction"), lookup(functions))
            assertEquals(setOf("Parent", "Parent.Nested"), lookup(classes))
            assertEquals(lookup(functions), lookup(LookupPredicate.create { all and functions }))
            assertEquals(lookup(functions), lookup(LookupPredicate.create { functions and all }))
            assertEquals(lookup(functions) + lookup(classes), lookup(LookupPredicate.create { functions or classes }))
            assertEquals(lookup(all), lookup(LookupPredicate.create { functions or all }))
            assertEquals(lookup(all), lookup(LookupPredicate.create { all or functions }))
            assertEquals(
                setOf("topLevelFunction"),
                lookup(LookupPredicate.create { (functions or classes).matching(DeclarationType.TOP_LEVEL_FUNCTION) }),
            )
            assertEquals(
                setOf("topLevelFunction"),
                lookup(LookupPredicate.create { functions.matching(DeclarationType.TOP_LEVEL_FUNCTION) }),
            )
            assertEquals(emptySet(), lookup(LookupPredicate.create { functions and classes }))
            assertEquals(
                setOf("Parent.Companion"),
                lookup(LookupPredicate.create { annotated(annotation).matching(DeclarationType.COMPANION_OBJECT) }),
            )

            val parentPsi = ktFile.declarations.filterIsInstance<KtClass>().single { it.name == "Parent" }
            val parent = parentPsi.resolveToFirSymbolOfType<FirRegularClassSymbol>(resolutionFacade)
            val member = parentPsi.declarations.filterIsInstance<KtNamedFunction>().single { it.name == "member" }
                .resolveToFirSymbolOfType<FirNamedFunctionSymbol>(resolutionFacade)
            assertTrue(provider.matches(LookupPredicate.create { parentAnnotated(annotation).matching(DeclarationType.MEMBER_FUNCTION) }, member))
            assertFalse(provider.matches(LookupPredicate.create { parentAnnotated(annotation).matching(DeclarationType.CLASS) }, member))
            assertTrue(provider.matches(LookupPredicate.create { hasAnnotated(annotation).matching(DeclarationType.CLASS_ONLY) }, parent))
            assertFalse(provider.matches(LookupPredicate.create { hasAnnotated(annotation).matching(DeclarationType.FUNCTION) }, parent))
            assertTrue(provider.matches(DeclarationPredicate.create { parentAnnotated(annotation).matching(DeclarationType.MEMBER_FUNCTION) }, member))
            assertTrue(provider.matches(DeclarationPredicate.create { hasAnnotated(annotation).matching(DeclarationType.CLASS_ONLY) }, parent))

            val metaAnnotation = FqName("org.jetbrains.kotlin.plugin.sandbox.AllOpen")
            val metaPredicate = DeclarationPredicate.create { metaAnnotated(metaAnnotation, includeItself = false) }
            val metaClasses = DeclarationPredicate.create { metaPredicate.matching(DeclarationType.CLASS_ONLY) }
            val registeredAnnotations = resolutionFacade.useSiteFirSession.registeredPluginAnnotations
            assertEquals(setOf(FqName("test.MetaMarker")), registeredAnnotations.getAnnotationsForPredicate(metaClasses))
            assertEquals(
                registeredAnnotations.getAnnotationsForPredicate(metaPredicate),
                registeredAnnotations.getAnnotationsForPredicate(metaClasses),
            )
            parent.lazyResolveToPhase(FirResolvePhase.STATUS)
            assertTrue(provider.matches(metaClasses, parent))
            assertFalse(provider.matches(DeclarationPredicate.create { metaPredicate.matching(DeclarationType.FUNCTION) }, parent))
        }
    }

    @Test
    fun matchingTypePredicateSkipsFir(ktFile: KtFile, testModule: KtTestModule) {
        val rejectedFile = testModule.ktFiles.single { it.name == "rejected.kt" }
        // The test infrastructure has already checked each fixture's FIR before invoking this test.
        clearCaches(ktFile.project)
        withResolutionFacade(ktFile) { resolutionFacade ->
            val session = resolutionFacade.useSiteFirSession as LLFirResolvableModuleSession
            val cache = session.moduleComponents.cache
            val provider = session.predicateBasedProvider
            val annotation = FqName("org.jetbrains.kotlin.plugin.sandbox.TestTopLevelPrivateSuspendFun")
            val functions = LookupPredicate.create { annotated(annotation).matching(DeclarationType.TOP_LEVEL_FUNCTION) }

            assertNull(cache.getCachedFirFile(rejectedFile))
            assertEquals(setOf("selected"), provider.getSymbolsByPredicate(functions).mapTo(mutableSetOf()) { it.predicateTestName() })
            assertNull(cache.getCachedFirFile(rejectedFile))

            val unfilteredAlternative = LookupPredicate.create { functions or annotated(annotation) }
            assertEquals(
                setOf("selected", "Rejected"),
                provider.getSymbolsByPredicate(unfilteredAlternative).mapTo(mutableSetOf()) { it.predicateTestName() },
            )
            assertNotNull(cache.getCachedFirFile(rejectedFile))
        }
    }

    /**
     * A regression test for KT-75685.
     *
     * The compiler never matches a plugin annotation used through a type alias (KT-69777),
     * so the answer must not depend on the amount of the performed resolution either.
     */
    @Test
    fun aliasedPluginAnnotation(ktFile: KtFile) {
        // The test infrastructure has already checked each fixture's FIR before invoking this test.
        clearCaches(ktFile.project)
        withResolutionFacade(ktFile) { resolutionFacade ->
            val provider = resolutionFacade.useSiteFirSession.predicateBasedProvider
            val annotation = FqName("org.jetbrains.kotlin.plugin.sandbox.AllOpen")
            val predicate = DeclarationPredicate.create { annotated(annotation) }
            val classes = ktFile.declarations.filterIsInstance<KtClass>().map {
                it.resolveToFirSymbolOfType<FirRegularClassSymbol>(resolutionFacade)
            }

            fun matchesAt(phase: FirResolvePhase): Map<String, Boolean> = classes.associate { symbol ->
                symbol.lazyResolveToPhase(phase)
                symbol.predicateTestName() to provider.matches(predicate, symbol)
            }

            assertEquals(
                mapOf("Aliased" to false, "AliasedWithDeprecated" to false, "Direct" to true),
                matchesAt(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS),
            )
            assertEquals(
                mapOf("Aliased" to false, "AliasedWithDeprecated" to false, "Direct" to true),
                matchesAt(FirResolvePhase.TYPES),
            )
            assertEquals(
                setOf("Direct"),
                provider.getSymbolsByPredicate(LookupPredicate.create { annotated(annotation) }).mapTo(mutableSetOf()) {
                    it.predicateTestName()
                },
            )
        }
    }

    @Test
    fun rootCompilerPluginAnnotation(ktFile: KtFile, testServices: TestServices) {
        withResolutionFacade(ktFile) { resolutionFacade ->
            val pluginAnnotations = resolutionFacade.useSiteFirSession.registeredPluginAnnotations
            val assertions = testServices.assertions
            assertions.assertTrue(pluginAnnotations is LLFirIdeRegisteredPluginAnnotations) {
                "The IDE implementation is expected"
            }

            assertions.assertEquals(
                expected = emptySet<FqName>(),
                actual = pluginAnnotations.getAnnotationsWithMetaAnnotation(FqName.ROOT),
            )
        }
    }
}

private fun FirBasedSymbol<*>.predicateTestName(): String = when (this) {
    is FirClassLikeSymbol<*> -> classId.relativeClassName.asString()
    is FirCallableSymbol<*> -> {
        val id = callableId ?: error("Unexpected callable without an id: $this")
        listOfNotNull(id.classId?.relativeClassName?.asString(), id.callableName.asString()).joinToString(".")
    }
    else -> error("Unexpected symbol $this")
}
