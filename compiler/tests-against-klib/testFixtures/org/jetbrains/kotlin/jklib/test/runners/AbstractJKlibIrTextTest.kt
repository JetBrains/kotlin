package org.jetbrains.kotlin.jklib.test.runners

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.commonHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.setupDefaultDirectivesForIrTextTest
import org.jetbrains.kotlin.test.configuration.setupIrTextDumpHandlers
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.configuration.commonServicesConfigurationForCodegenAndDebugTest
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.klibArtifactsHandlersStep
import org.jetbrains.kotlin.utils.bind

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmForeignAnnotationsConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.fir.FirSpecificParserSuppressor


abstract class AbstractJKlibIrTextTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>>(
    val targetFrontend: FrontendKind<FrontendOutput>
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    abstract val frontendFacade: Constructor<FrontendFacade<FrontendOutput>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FrontendOutput, IrBackendInput>>
    abstract val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        // Custom configuration for JKlib
        // Replicated from commonServicesConfigurationForCodegenAndDebugTest but without ScriptingEnvironmentConfigurator
        globalDefaults {
            frontend = targetFrontend
            targetBackend = TargetBackend.JVM_IR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            artifactKind = ArtifactKinds.KLib
            dependencyKind = DependencyKind.Binary
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JKlibSourceRootConfigurator,
            ::JKlibJavaSourceConfigurator,
        )

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useMetaTestConfigurators(::FirSpecificParserSuppressor, ::WithStdlibSkipper /*, ::MuteListSkipper */)

        facadeStep(frontendFacade)
        firHandlersStep {
            useHandlers(::NoFirCompilationErrorsHandler)
        }

        facadeStep(frontendToBackendConverter)
        irHandlersStep()

        facadeStep(backendFacade)
        klibArtifactsHandlersStep()

        setupDefaultDirectivesForIrTextTest()
        configureIrHandlersStep {
            setupIrTextDumpHandlers()
        }

        useAfterAnalysisCheckers(
            ::BlackBoxCodegenSuppressor,
            ::PhasedPipelineChecker.bind(TestPhase.BACKEND)
        )
        enableMetaInfoHandler()
    }
}

class WithStdlibSkipper(testServices: org.jetbrains.kotlin.test.services.TestServices) : org.jetbrains.kotlin.test.services.MetaTestConfigurator(testServices) {
    override val directiveContainers: List<org.jetbrains.kotlin.test.directives.model.DirectivesContainer>
        get() = listOf(org.jetbrains.kotlin.test.directives.ConfigurationDirectives)

    override fun shouldSkipTest(): Boolean {
        return testServices.moduleStructure.allDirectives.contains(org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB)
    }
}

class MuteListSkipper(testServices: org.jetbrains.kotlin.test.services.TestServices) : org.jetbrains.kotlin.test.services.MetaTestConfigurator(testServices) {
    companion object {
        private val mutedTests = setOf(
            "testAllPropertiesAndMethodsKJ",
            "testAllPropertiesAndMethodsKJJ",
            "testAllPropertiesAndMethodsKJK",
            "testAllPropertiesAndMethodsKJKJ",
            "testAllPropertiesAndMethodsWithSeparateModuleKJJ",
            "testAmbiguousFieldAccess",
            "testAnnotationInAnnotation",
            "testAnnotationMixedTargeting",
            "testAnnotationRetentions",
            "testAnnotationRetentionsMultiModule",
            "testArgWithDefaultValueInAnnotationClass",
            "testArgumentMappedWithError",
            "testArrayAccess",
            "testArrayAssignment",
            "testArrayAugmentedAssignment1",
            "testArrayInAnnotationArguments",
            "testArrayListOverrides",
            "testArraysFromBuiltins",
            "testAsOnPlatformType",
            "testAssignmentOperator",
            "testBadBreakContinue",
            "testBaseOverrideOnNothing",
            "testBasicGenericWithBoundsOverride",
            "testBasicJavaModifiersOverride",
            "testBasicJavaThisOverride",
            "testBasicKotlinDefaultParametersOverride",
            "testBasicKotlinModifiersOverride",
            "testBasicNullabilityAnnotationOverride",
            "testBasicOperatorsOverride",
            "testBasicOverride",
            "testBasicOverrideOnComplexHierarchy",
            "testBasicOverrideOnComplexHierarchy",
            "testBasicVoidOverride",
            "testBoundCallableReferences",
            "testBreakContinueInLoopHeader",
            "testCapturedTypeInFakeOverride",
            "testClassLiteralInAnnotation",
            "testCoercionInLoop",
            "testCoercionToUnit",
            "testCombinationExplicitImlicitOverride",
            "testComplexAugmentedAssignment",
            "testConstExpressionsInAnnotationArguments",
            "testConstFromBuiltins",
            "testConstValInitializers",
            "testConstructorWithOwnTypeParametersCall",
            "testContextWithAnnotation",
            "testDataClassMethodOverride",
            "testDefinitelyNotNullWithIntersection1",
            "testDelegateForExtPropertyInClass",
            "testDelegatedSetterShouldBeSpecialized",
            "testDelegatedImplementationOfJavaInterface",
            "testDelegatedPropertyAccessorsWithAnnotations",
            "testDelegatedSetterShouldBeSpecialized",
            "testDelegationAndInheritanceFromJava",
            "testEnhancedNullability",
            "testEnumMethodOverride",
            "testEquals",
            "testExclExclOnPlatformType",
            "testExpectClassInherited",
            "testExpectIntersectionOverride",
            "testExpectMemberInNotExpectClass",
            "testExpectedEnumClass",
            "testExpectedFun",
            "testExpectedSealedClass",
            "testExplicitEqualsAndCompareToCallsOnPlatformTypeReceiver",
            "testExplicitIncrement",
            "testExtensionLambda",
            "testFakeOverrideOfRaw",
            "testFakeOverrideOfRawJavaCollection",
            "testFakeOverridesForJavaNonStaticMembers",
            "testFakeOverridesForJavaStaticMembers",
            "testFieldAccess_generic",
            "testFieldAccess_invisible",
            "testFieldAccess_regular",
            "testFieldsFromJavaClass",
            "testFileAnnotations",
            "testFirBuilder",
            "testFloatingPointCompareTo",
            "testFloatingPointLess",
            "testFunInterfaceConstructorReference",
            "testFunWithDefaultParametersAsKCallableStar",
            "testFunctionLiteralGenericSignature",
            "testGenericAnnotationClasses",
            "testGenericClassInDifferentModule",
            "testGenericDelegatedProperty",
            "testGenericMember",
            "testGenericPropertyRef",
            "testGenericPropertyReferenceType",
            "testGenericSamSmartcast",
            "testGenericWithBoundsOnComplexHierarchy",
            "testGenericWithBoundsOnKJJ",
            "testGenericWithBoundsOnKJK",
            "testGetterSetterOverrideOnKJ",
            "testGetterSetterOverrideOnKJK",
            "testGetterSetterOverrideonComplexHierarchy",
            "testGetterSetterOvverideOnKJJ",
            "testGetterSetterOvverrideOnKJKJ",
            "testIfWithArrayOperation",
            "testImplicitCastOnPlatformType",
            "testImplicitNotNullInDestructuringAssignment",
            "testImplicitNotNullInDestructuringAssignmentOld",
            "testImplicitNotNullOnDelegatedImplementation",
            "testImplicitNotNullOnPlatformType",
            "testImportedFromObject",
            "testIncrementDecrement",
            "testIndependentBackingFieldType",
            "testInheritJavaListSubclass",
            "testInheritJavaUtilList",
            "testInitValInLambda",
            "testIntegerCoercionToT",
            "testInternalOverrideCrossModule",
            "testInternalOverrideWithFriendModule",
            "testInternalPotentialFakeOverride",
            "testInternalPotentialOverride",
            "testInternalWithPublishedApiOverride",
            "testIntersectionFakeOverrides_nonTransitiveTypeSystem",
            "testIntersectionFakeOverrides_nonTransitiveTypeSystem2",
            "testIntersectionFakeOverrides_nonTransitiveTypeSystem3",
            "testIntersectionFakeOverrides_operators",
            "testIntersectionFakeOverrides_operators3",
            "testIntersectionGenericWithBoundsOverride",
            "testIntersectionJavaModifiersOverride",
            "testIntersectionJavaThisOverride",
            "testIntersectionKotlinDefaultParametersOverride",
            "testIntersectionKotlinModifiersOverride",
            "testIntersectionListOverrideKJJ",
            "testIntersectionListOverrideKJK",
            "testIntersectionNullabilityAnnotation",
            "testIntersectionOnComplexHierarchy",
            "testIntersectionOperatorsOverride",
            "testIntersectionOverride",
            "testIntersectionOverrideForGetterSetter",
            "testIntersectionOverrideOnStaticKJJ",
            "testIntersectionOverrideOnStaticKJK",
            "testIntersectionOverrrideOnNothing",
            "testIntersectionType1",
            "testIntersectionVisibilityOverrideKJJ",
            "testIntersectionVisibilityOverrideKJK",
            "testIntersectionVoidOverride",
            "testIntersectionWithExplicitOverride",
            "testIntersectionWithGenericExplicitOverride",
            "testIntersectionWithGenericOnComplexHierarchy",
            "testIntersectionWithGenericOnComplexHierarchy",
            "testIntersectionWithGenericOverride",
            "testIntersectionWithGenericOverride",
            "testIntersectionWithJava",
            "testIntersectionWithMappedSignature",
            "testIntersectionWithPublishedApiOverride",
            "testIntersectionWithRawType",
            "testIntersectionWithSeparateModule",
            "testJavaAnnotation",
            "testJavaAnnotationOnJavaEnum",
            "testJavaAnnotationWithSingleArrayArgument",
            "testJavaConstructorWithTypeParameters",
            "testJavaEnum",
            "testJavaInnerClass",
            "testJavaMethod",
            "testJavaModifiersOverride",
            "testJavaNestedClass",
            "testJavaNestedClassesInHierarchy",
            "testJavaNumberOverride",
            "testJavaRawTypesAndGenericsErasure",
            "testJavaRecordComponentAccess",
            "testJavaStaticMethod",
            "testJavaSyntheticGenericPropretyAccess",
            "testJavaSyntheticProperty",
            "testJavaSyntheticPropertyAccess",
            "testJavaWildcardType",
            "testJdkClassSyntheticProperty",
            "testJvmFieldReferenceWithIntersectionTypes",
            "testJvmInstanceFieldReference",
            "testJvmStaticFieldReference",
            "testKjkWithRawTypes",
            "testKotlinCharSequenceOverride",
            "testKotlinCustomAnnotationOverride",
            "testKotlinExtensionsOverride",
            "testKotlinFunParamModifiersOverride",
            "testKotlinModifiersOverride",
            "testKotlinNumberOverride",
            "testKt16904",
            "testKt27005",
            "testKt28006",
            "testKt28456",
            "testKt28456b",
            "testKt29833",
            "testKt36956",
            "testKt37570",
            "testKt37779",
            "testKt43217",
            "testKt44855",
            "testKt45853",
            "testKt46069",
            "testKt50028",
            "testKt65236",
            "testKt65432",
            "testLambdaInCAO",
            "testLambdaWithParameterName",
            "testLateinitPropertiesSeparateModule",
            "testListOverrideKJ",
            "testListOverrideKJJ",
            "testListOverrideOnKJKJ",
            "testLocal",
            "testLocalFunction",
            "testMember",
            "testMemberExtension",
            "testMemberExtensionOverride",
            "testModality",
            "testMultiList",
            "testMultipleImplicitReceivers",
            "testNewInferenceFixationOrder1",
            "testNnStringVsT",
            "testNnStringVsTAny",
            "testNnStringVsTConstrained",
            "testNnStringVsTXArray",
            "testNnStringVsTXString",
            "testNoErrorTypeAfterCaptureApproximation",
            "testNullCheckInElvisRhs",
            "testNullCheckOnGenericLambdaReturn",
            "testNullCheckOnInterfaceDelegation",
            "testNullCheckOnLambdaReturn",
            "testNullabilityAnnotationOverrideOnComplexHierarchy",
            "testNullabilityAssertionOnExtensionReceiver",
            "testNullableAnyAsIntToDouble",
            "testOutBox",
            "testOverrideKotlinMethodsKJKK",
            "testOverridePropertiesKJKK",
            "testOverrideSetAndGetKJK",
            "testPlatformTypeReceiver",
            "testPlatformTypesFakeOverrides",
            "testPlatformTypesFakeOverrides2",
            "testPlatformTypesOverrides",
            "testPropertyReferences",
            "testProtectedJavaFieldRef",
            "testRawTypeInSignature",
            "testReceiverOfIntersectionType",
            "testReflectFindAnnotationOnDefaultMethodParameter",
            "testReflectGetOnNullableTypeAlias",
            "testReflectionLiterals",
            "testSafeCallWithIncrementDecrement",
            "testSamByProjectedType",
            "testSamConstructors",
            "testSamConversionInGenericConstructorCall",
            "testSamConversionToGeneric",
            "testSamConversions",
            "testSamConversionsWithSmartCasts",
            "testSamOperators",
            "testSameJavaFieldReferences",
            "testSetFieldWithImplicitCast",
            "testSetterVisibliityWithJava",
            "testSignatureComputationComplexJavaGeneric",
            "testSmartCastOnFieldReceiverOfGenericType",
            "testSpecialAnnotationsMetadata",
            "testSpreadOperatorInAnnotationArguments",
            "testStaticOverrideOnComplexHierarchy",
            "testStaticOverrideOnKJ",
            "testStaticOverrideOnKJJ",
            "testStaticOverrideOnKJK",
            "testStaticOverrideOnKJKJ",
            "testStringVsAny",
            "testStringVsT",
            "testStringVsTAny",
            "testStringVsTConstrained",
            "testStringVsTXArray",
            "testStringVsTXString",
            "testSubstitutionFakeOverrides",
            "testSubstitutionOperatorsOverride",
            "testSubstitutionOverride",
            "testSubstitutionOverrideForGetterSetter",
            "testSubstitutionOverrideOnComplexHierarchy",
            "testSubstitutionOverrideOnNothing",
            "testSubstitutionStaticOverride",
            "testTargetOnPrimaryCtorParameter",
            "testTopLevel",
            "testTypeAliasesWithAnnotations",
            "testTypeParameterAnnotationOverride",
            "testTypeParameterBounds",
            "testTypeParameterClassLiteral",
            "testTypeParameterInClashingAccessor",
            "testTypeParameterWithMixedNullableAndNotNullableBounds",
            "testTypeParameterWithMultipleNotNullableBounds",
            "testTypeParameterWithMultipleNullableBounds",
            "testTypeParametersWithAnnotations",
            "testV8ArrayToList",
            // "testVararg",
            "testVarargWithImplicitCast",
            "testWhenReturnUnit",
            "testWhileDoWhile",
            "testWithVarargViewedAsArray",
        )
    }

    override val directiveContainers: List<org.jetbrains.kotlin.test.directives.model.DirectivesContainer>
        get() = emptyList()

    override fun shouldSkipTest(): Boolean {
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val name = originalFile.nameWithoutExtension
        val testName = "test" + name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        return testName in mutedTests
    }
}
