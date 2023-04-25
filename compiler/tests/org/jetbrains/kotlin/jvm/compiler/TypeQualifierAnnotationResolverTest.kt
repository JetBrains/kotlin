/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.checkers.FOREIGN_ANNOTATIONS_SOURCES_PATH
import org.jetbrains.kotlin.checkers.JSR_305_SOURCES_PATH
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.AnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
import org.jetbrains.kotlin.load.java.lazy.JavaResolverComponents
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.AnnotationArgumentsRenderingPolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtilExt
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class TypeQualifierAnnotationResolverTest : KtUsefulTestCase() {
    companion object {
        private const val TEST_DATA_PATH = "compiler/testData/typeQualifierNickname/"
    }

    fun testBasicJSRNullabilityAnnotations() {
        val (typeQualifierResolver, aClass) = buildTypeQualifierResolverAndFindClass("A")

        assertMethodHasUnwrappedAnnotation(
            aClass, typeQualifierResolver,
            "nullable",
            "@javax.annotation.Nonnull(when = When.UNKNOWN)"
        )

        assertMethodHasUnwrappedAnnotation(
            aClass, typeQualifierResolver,
            "checkForNull",
            "@javax.annotation.CheckForNull()"
        )

        assertMethodHasUnwrappedAnnotation(
            aClass, typeQualifierResolver,
            "nonNull",
            "@javax.annotation.Nonnull()"
        )

        assertMethodHasUnwrappedAnnotation(
            aClass, typeQualifierResolver,
            "nonNullExplicitArgument",
            "@javax.annotation.Nonnull(when = When.ALWAYS)"
        )
    }

    fun testCustomNullabilityAnnotation() {
        val (typeQualifierResolver, aClass) = buildTypeQualifierResolverAndFindClass("B")

        assertMethodHasUnwrappedAnnotation(
            aClass, typeQualifierResolver,
            "myNullable",
            "@javax.annotation.CheckForNull()"
        )
    }

    private fun buildTypeQualifierResolverAndFindClass(className: String): Pair<AnnotationTypeQualifierResolver, ClassDescriptor> {
        val jsr305Jar = MockLibraryUtilExt.compileJavaFilesLibraryToJar(JSR_305_SOURCES_PATH, "jsr305")
        val configuration = KotlinTestUtils.newConfiguration(
            ConfigurationKind.ALL, TestJdkKind.FULL_JDK,
            listOf(
                KtTestUtil.getAnnotationsJar(),
                MockLibraryUtilExt.compileJavaFilesLibraryToJar(
                    FOREIGN_ANNOTATIONS_SOURCES_PATH,
                    "foreign-annotations",
                    extraClasspath = listOf(
                        jsr305Jar.absolutePath
                    )
                ),
                jsr305Jar,
            ),
            listOf(File(TEST_DATA_PATH))
        ).apply {
            languageVersionSettings = LanguageVersionSettingsImpl(
                LanguageVersion.LATEST_STABLE,
                ApiVersion.LATEST_STABLE,
                mapOf(JvmAnalysisFlags.javaTypeEnhancementState to JavaTypeEnhancementState.DEFAULT)
            )
        }

        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val container = JvmResolveUtil.createContainer(environment)
        val typeQualifierResolver = container.get<JavaResolverComponents>().annotationTypeQualifierResolver

        val aClass = container.get<ModuleDescriptor>().resolveClassByFqName(FqName(className), NoLookupLocation.FROM_TEST)!!

        return typeQualifierResolver to aClass
    }

    private fun assertMethodHasUnwrappedAnnotation(
        aClass: ClassDescriptor,
        typeQualifierResolver: AnnotationTypeQualifierResolver,
        methodName: String,
        annotationText: String
    ) {
        assertEquals(
            annotationText,
            DescriptorRenderer.withOptions {
                annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.ALWAYS_PARENTHESIZED
            }.renderAnnotation(
                aClass.findSingleTypeQualifierAnnotationOnMethod(methodName, typeQualifierResolver)
            )
        )
    }

    private fun ClassDescriptor.findSingleTypeQualifierAnnotationOnMethod(
        name: String,
        typeQualifierResolver: AnnotationTypeQualifierResolver
    ): AnnotationDescriptor = unsubstitutedMemberScope
        .getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_TEST)
        .single()
        .annotations.single()
        .let(typeQualifierResolver::resolveTypeQualifierAnnotation)
        .also(::assertNotNull)!!
}
